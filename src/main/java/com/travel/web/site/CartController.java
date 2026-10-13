package com.travel.web.site;

import com.travel.module5_packages.entity.TourPackage;
import com.travel.module5_packages.repository.TourPackageRepository;
import com.travel.module5_packages.service.BookingService;
import com.travel.module5_packages.service.BookingService.BookingRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The cart: anyone can add packages; signing in is only needed at checkout. */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final Cart cart;
    private final TourPackageRepository packageRepository;
    private final BookingService bookingService;

    public CartController(Cart cart, TourPackageRepository packageRepository, BookingService bookingService) {
        this.cart = cart;
        this.packageRepository = packageRepository;
        this.bookingService = bookingService;
    }

    /** One cart row with its current price. */
    public record Line(Cart.Item item, TourPackage pkg, BigDecimal total, List<String> problems) {
        public boolean isOk() { return problems.isEmpty(); }
    }

    @GetMapping
    public String view(Model model) {
        List<Line> lines = lines(cart, packageRepository, bookingService);
        model.addAttribute("lines", lines);
        model.addAttribute("grandTotal", total(lines));
        model.addAttribute("allOk", lines.stream().allMatch(Line::isOk));
        model.addAttribute("earliestDate", BookingService.earliestTravelDate());
        model.addAttribute("maxTravellers", BookingService.MAX_TRAVELLERS);
        return "site/cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam Integer packageId,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
                      @RequestParam(required = false, defaultValue = "1") Integer travellers,
                      RedirectAttributes flash) {
        BookingRequest request = new BookingRequest(packageId, travelDate, travellers == null ? 1 : travellers);
        List<String> problems = bookingService.check(request);
        if (!problems.isEmpty()) {
            flash.addFlashAttribute("errors", problems);
            return "redirect:/tours/" + packageId + "#book";
        }
        try {
            cart.add(new Cart.Item(packageId, travelDate, request.travellers()));
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
        flash.addFlashAttribute("success", "Added to your cart.");
        return "redirect:/cart";
    }

    @PostMapping("/{itemId}/update")
    public String update(@PathVariable String itemId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
                         @RequestParam(required = false, defaultValue = "1") Integer travellers,
                         RedirectAttributes flash) {
        cart.find(itemId).ifPresent(item -> {
            List<String> problems = bookingService.check(new BookingRequest(item.getPackageId(), travelDate,
                    travellers == null ? 1 : travellers));
            if (problems.isEmpty()) {
                item.setTravelDate(travelDate);
                item.setTravellers(travellers);
                flash.addFlashAttribute("success", "Cart updated.");
            } else {
                flash.addFlashAttribute("errors", problems);
            }
        });
        return "redirect:/cart";
    }

    @PostMapping("/{itemId}/remove")
    public String remove(@PathVariable String itemId, RedirectAttributes flash) {
        cart.remove(itemId);
        flash.addFlashAttribute("success", "Removed from your cart.");
        return "redirect:/cart";
    }

    static List<Line> lines(Cart cart, TourPackageRepository packageRepository, BookingService bookingService) {
        List<Line> lines = new ArrayList<>();
        for (Cart.Item item : cart.getItems()) {
            TourPackage pkg = packageRepository.findById(item.getPackageId()).orElse(null);
            if (pkg == null) {
                cart.remove(item.getId()); // package was deleted by staff
                continue;
            }
            BigDecimal total = pkg.getPrice().multiply(BigDecimal.valueOf(item.getTravellers()));
            List<String> problems = bookingService.check(new BookingRequest(item.getPackageId(), item.getTravelDate(), item.getTravellers()));
            lines.add(new Line(item, pkg, total, problems));
        }
        return lines;
    }

    static BigDecimal total(List<Line> lines) {
        return lines.stream().map(Line::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
