package com.travel.module5_packages.controller;

import com.travel.module5_packages.service.BookingService;
import com.travel.module5_packages.service.BookingStatusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BookingAdminController {

    private final BookingService bookingService;
    private final BookingStatusService statusService;

    public BookingAdminController(BookingService bookingService, BookingStatusService statusService) {
        this.bookingService = bookingService;
        this.statusService = statusService;
    }

    @GetMapping("/packages/bookings")
    public String bookings(@RequestParam(required = false, defaultValue = "ALL") String show, Model model) {
        var views = statusService.views(bookingService.all());
        if (!"ALL".equals(show)) {
            views = views.stream().filter(v -> v.state().equals(show)).toList();
        }
        model.addAttribute("bookings", views);
        model.addAttribute("show", show);
        return "module5_packages/bookings";
    }

    @PostMapping("/packages/bookings/{id}/cancel")
    public String cancel(@PathVariable Integer id, RedirectAttributes flash) {
        bookingService.setCancelled(id, true);
        flash.addFlashAttribute("successMessage", "Booking cancelled. Refunds, if any, are handled in Billing.");
        return "redirect:/packages/bookings";
    }

    @PostMapping("/packages/bookings/{id}/restore")
    public String restore(@PathVariable Integer id, RedirectAttributes flash) {
        bookingService.setCancelled(id, false);
        flash.addFlashAttribute("successMessage", "Booking restored.");
        return "redirect:/packages/bookings";
    }
}
