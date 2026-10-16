package com.travel.web.site;

import com.travel.common.ValidationException;
import com.travel.module1_auth.domain.SiteCustomerEntity;
import com.travel.module1_auth.service.SiteCustomerService;
import com.travel.module5_packages.repository.TourPackageRepository;
import com.travel.module5_packages.service.BookingService;
import com.travel.module5_packages.service.BookingService.BookingRequest;
import com.travel.module5_packages.service.BookingService.CheckoutResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CheckoutController {

    private final Cart cart;
    private final TourPackageRepository packageRepository;
    private final BookingService bookingService;
    private final SiteCustomerService customerService;

    public CheckoutController(Cart cart, TourPackageRepository packageRepository, BookingService bookingService,
                              SiteCustomerService customerService) {
        this.cart = cart;
        this.packageRepository = packageRepository;
        this.bookingService = bookingService;
        this.customerService = customerService;
    }

    @GetMapping("/checkout")
    public String checkout(Model model, RedirectAttributes flash) {
        if (cart.isEmpty()) {
            flash.addFlashAttribute("info", "Your cart is empty. Pick a tour to get started.");
            return "redirect:/tours";
        }
        List<CartController.Line> lines = CartController.lines(cart, packageRepository, bookingService);
        if (!lines.stream().allMatch(CartController.Line::isOk)) {
            flash.addFlashAttribute("error", "Please fix the highlighted items in your cart first.");
            return "redirect:/cart";
        }
        model.addAttribute("lines", lines);
        model.addAttribute("grandTotal", CartController.total(lines));
        return "site/checkout";
    }

    @PostMapping("/checkout")
    public String placeBooking(@RequestParam(required = false) String notes,
                               @RequestParam(required = false) String agree,
                               RedirectAttributes flash) {
        if (agree == null) {
            flash.addFlashAttribute("error", "Please accept the booking terms to continue.");
            return "redirect:/checkout";
        }
        SiteCustomerEntity customer = customerService.requireCurrent();
        List<BookingRequest> requests = cart.getItems().stream()
                .map(i -> new BookingRequest(i.getPackageId(), i.getTravelDate(), i.getTravellers()))
                .toList();
        try {
            CheckoutResult result = bookingService.checkout(customer, requests, notes);
            cart.clear();
            flash.addFlashAttribute("success", "Booking received! Invoice " + result.invoice().getInvoiceNumber()
                    + " is ready - choose how you'd like to pay.");
            return "redirect:/my/invoices/" + result.invoice().getInvoiceId();
        } catch (ValidationException e) {
            flash.addFlashAttribute("errors", e.getErrors());
            return "redirect:/cart";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }
}
