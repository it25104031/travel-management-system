package com.travel.module5_packages;

import com.travel.module3_billing.repository.InvoiceRepository;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentSlipService;
import com.travel.module5_packages.entity.TourBooking;
import com.travel.module5_packages.repository.TourBookingRepository;
import com.travel.module5_packages.repository.TourPackageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebsiteBookingFlowTest {

    @Autowired MockMvc mvc;
    @Autowired TourPackageRepository packages;
    @Autowired TourBookingRepository bookings;
    @Autowired InvoiceRepository invoices;
    @Autowired InvoiceService invoiceService;
    @Autowired PaymentService paymentService;
    @Autowired PaymentSlipService slipService;

    @Test
    void travellerBooksATourAndPaysByBankSlip() throws Exception {
        Integer packageId = packages.findAll().get(0).getPackageId();
        BigDecimal price = packages.findById(packageId).orElseThrow().getPrice();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("packageId", packageId.toString())
                        .param("travelDate", LocalDate.now().plusDays(30).toString())
                        .param("travellers", "2"))
                .andExpect(redirectedUrl("/cart"));
        mvc.perform(get("/checkout").session(session)).andExpect(status().is3xxRedirection());

        mvc.perform(post("/signup").session(session).with(csrf())
                        .param("fullName", "Kamala Silva").param("email", "kamala@example.com")
                        .param("password", "travel123").param("confirmPassword", "travel123")
                        .param("continue", "/checkout"))
                .andExpect(redirectedUrl("/checkout"));
        mvc.perform(get("/checkout").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Place booking")));

        String location = mvc.perform(post("/checkout").session(session).with(csrf()).param("agree", "yes"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertNotNull(location);
        assertTrue(location.startsWith("/my/invoices/"));
        Integer invoiceId = Integer.valueOf(location.substring("/my/invoices/".length()));

        List<TourBooking> booked = bookings.findByInvoiceId(invoiceId);
        assertEquals(1, booked.size());
        assertEquals(0, price.multiply(BigDecimal.valueOf(2)).compareTo(invoices.findById(invoiceId).orElseThrow().getTotalAmount()));

        assertThrows(IllegalStateException.class, () -> invoiceService.deleteInvoice(invoiceId));

        mvc.perform(post(location + "/card").session(session).with(csrf()).param("amount", "1000"))
                .andExpect(status().is4xxClientError());
        mvc.perform(get(location).session(session)).andExpect(content().string(containsString("coming soon")));

        BigDecimal due = paymentService.getOutstandingBalance(invoiceId);
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 0};
        mvc.perform(multipart(location + "/slip")
                        .file(new MockMultipartFile("file", "slip.png", "image/png", png))
                        .param("amountPaid", due.toPlainString())
                        .param("paymentDate", LocalDate.now().toString())
                        .param("bankReference", "TRX-1")
                        .session(session).with(csrf()))
                .andExpect(flash().attributeExists("success"));
        mvc.perform(get("/my").session(session)).andExpect(content().string(containsString("Payment in review")));

        Integer slipId = slipService.getPaymentSlipsForInvoice(invoiceId).get(0).getSlipId();
        slipService.approveSlip(slipId, "Received");
        assertEquals("PAID", paymentService.getInvoiceStatus(invoiceId));

        mvc.perform(get("/my").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmed")));

        Integer paidBookingId = booked.get(0).getBookingId();
        mvc.perform(post("/my/trips/" + paidBookingId + "/delete").session(session).with(csrf()))
                .andExpect(redirectedUrl("/my?tab=trips")).andExpect(flash().attributeExists("error"));
        assertTrue(bookings.findById(paidBookingId).isPresent());
    }

    @Test
    void travellerDeletesUnpaidTrips() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Integer p1 = packages.findAll().get(0).getPackageId();
        Integer p2 = packages.findAll().get(1).getPackageId();
        mvc.perform(post("/signup").session(session).with(csrf())
                .param("fullName", "Nimal Perera").param("email", "nimal@example.com")
                .param("password", "travel123").param("confirmPassword", "travel123"));
        for (Integer id : List.of(p1, p2)) {
            mvc.perform(post("/cart/add").session(session).with(csrf()).param("packageId", id.toString())
                    .param("travelDate", LocalDate.now().plusDays(20).toString()).param("travellers", "1"));
        }
        String location = mvc.perform(post("/checkout").session(session).with(csrf()).param("agree", "yes"))
                .andReturn().getResponse().getRedirectedUrl();
        Integer invoiceId = Integer.valueOf(location.substring("/my/invoices/".length()));
        List<TourBooking> booked = bookings.findByInvoiceId(invoiceId);
        assertEquals(2, booked.size());
        mvc.perform(get("/my").session(session)).andExpect(content().string(containsString("Delete trip")));

        MockHttpSession other = new MockHttpSession();
        mvc.perform(post("/signup").session(other).with(csrf())
                .param("fullName", "Someone Else").param("email", "someone@example.com")
                .param("password", "travel123").param("confirmPassword", "travel123"));
        mvc.perform(post("/my/trips/" + booked.get(0).getBookingId() + "/delete").session(other).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertEquals(2, bookings.findByInvoiceId(invoiceId).size());

        TourBooking first = booked.get(0), second = booked.get(1);
        mvc.perform(post("/my/trips/" + first.getBookingId() + "/delete").session(session).with(csrf()))
                .andExpect(redirectedUrl("/my?tab=trips")).andExpect(flash().attribute("success", "Trip deleted."));
        assertFalse(bookings.findById(first.getBookingId()).isPresent());
        assertEquals(0, second.getTotalAmount().compareTo(invoices.findById(invoiceId).orElseThrow().getTotalAmount()));

        mvc.perform(post("/my/trips/" + second.getBookingId() + "/delete").session(session).with(csrf()))
                .andExpect(flash().attribute("success", "Trip deleted."));
        assertTrue(bookings.findByInvoiceId(invoiceId).isEmpty());
        assertFalse(invoices.findById(invoiceId).isPresent());
    }

    @Test
    void anotherTravellerCannotOpenSomeoneElsesInvoice() throws Exception {
        MockHttpSession owner = new MockHttpSession();
        Integer packageId = packages.findAll().get(1).getPackageId();
        mvc.perform(post("/signup").session(owner).with(csrf())
                .param("fullName", "Owner One").param("email", "owner@example.com")
                .param("password", "travel123").param("confirmPassword", "travel123"));
        mvc.perform(post("/cart/add").session(owner).with(csrf()).param("packageId", packageId.toString())
                .param("travelDate", LocalDate.now().plusDays(10).toString()).param("travellers", "1"));
        String location = mvc.perform(post("/checkout").session(owner).with(csrf()).param("agree", "yes"))
                .andReturn().getResponse().getRedirectedUrl();

        MockHttpSession other = new MockHttpSession();
        mvc.perform(post("/signup").session(other).with(csrf())
                .param("fullName", "Other Person").param("email", "other@example.com")
                .param("password", "travel123").param("confirmPassword", "travel123"));
        mvc.perform(get(location).session(other)).andExpect(status().isNotFound());
    }
}
