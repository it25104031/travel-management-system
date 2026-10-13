package com.travel.web.staff;

import com.travel.module1_auth.repository.SiteCustomerRepository;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.service.VisaApplicationService;
import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.repository.PaymentSlipRepository;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;
import com.travel.module4_logistics.entity.Departure;
import com.travel.module4_logistics.service.LogisticsService;
import com.travel.module5_packages.repository.TourPackageRepository;
import com.travel.module5_packages.service.BookingService;
import com.travel.module5_packages.service.BookingStatusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Staff home: one overview across all five modules. */
@Controller
public class StaffHomeController {

    private final VisaApplicationService visaService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final PaymentSlipRepository slipRepository;
    private final LogisticsService logisticsService;
    private final TourPackageRepository packageRepository;
    private final BookingService bookingService;
    private final BookingStatusService bookingStatusService;
    private final SiteCustomerRepository customerRepository;

    public StaffHomeController(VisaApplicationService visaService, InvoiceService invoiceService,
                               PaymentService paymentService, PaymentSlipRepository slipRepository,
                               LogisticsService logisticsService, TourPackageRepository packageRepository,
                               BookingService bookingService, BookingStatusService bookingStatusService,
                               SiteCustomerRepository customerRepository) {
        this.visaService = visaService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.slipRepository = slipRepository;
        this.logisticsService = logisticsService;
        this.packageRepository = packageRepository;
        this.bookingService = bookingService;
        this.bookingStatusService = bookingStatusService;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/staff")
    public String home(Model model) {
        LocalDate today = LocalDate.now();

        List<VisaApplicationEntity> visas = visaService.findAll();
        long openVisas = visas.stream().filter(v -> !v.getStatusEnum().isClosed()).count();

        List<Invoice> invoices = invoiceService.getAllInvoices();
        Map<Integer, InvoiceSummary> summaries = paymentService.getSummaries(invoices);
        long unpaid = summaries.values().stream().filter(s -> !"PAID".equals(s.status())).count();
        BigDecimal outstanding = summaries.values().stream().map(InvoiceSummary::outstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Departure> upcomingDepartures = logisticsService.listDepartures().stream()
                .filter(d -> !d.getEndDate().isBefore(today)).limit(5).toList();

        var bookings = bookingStatusService.views(bookingService.all());

        model.addAttribute("openVisas", openVisas);
        model.addAttribute("totalVisas", visas.size());
        model.addAttribute("unpaidInvoices", unpaid);
        model.addAttribute("outstanding", PaymentService.formatMoney(outstanding));
        model.addAttribute("revenue", PaymentService.formatMoney(paymentService.getTotalRevenue()));
        model.addAttribute("pendingSlips", slipRepository.countByStatus("PENDING"));
        model.addAttribute("upcomingDepartures", upcomingDepartures);
        model.addAttribute("supplierCount", logisticsService.listSuppliers().size());
        model.addAttribute("packageCount", packageRepository.count());
        model.addAttribute("availablePackages", packageRepository.findAll().stream().filter(p -> p.isAvailable()).count());
        model.addAttribute("activeBookings", bookings.stream().filter(b -> !b.booking().isCancelled()).count());
        model.addAttribute("recentBookings", bookings.stream().limit(6).toList());
        model.addAttribute("travellerCount", customerRepository.count());
        model.addAttribute("recentVisas", visas.stream()
                .sorted((a, b) -> b.getCreatedAt() == null || a.getCreatedAt() == null ? 0 : b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5).toList());
        return "web/staff-home";
    }
}
