package com.travel.web.site;

import com.travel.module5_packages.entity.TourPackage;
import com.travel.module5_packages.repository.TourPackageRepository;
import com.travel.module5_packages.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Public website: home, about us and the tour package catalogue. */
@Controller
public class SiteController {

    private final TourPackageRepository packageRepository;

    public SiteController(TourPackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<TourPackage> available = available();
        model.addAttribute("featured", available.stream().limit(6).toList());
        model.addAttribute("packageCount", available.size());
        return "site/home";
    }

    @GetMapping("/about")
    public String about() {
        return "site/about";
    }

    @GetMapping("/tours")
    public String tours(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false, defaultValue = "recommended") String sort,
                        Model model) {
        List<TourPackage> all = available();
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<TourPackage> shown = all.stream()
                .filter(p -> query.isEmpty() || contains(p.getPackageName(), query) || contains(p.getDestination(), query)
                        || contains(p.getDescription(), query))
                .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.getCategory()))
                .sorted(switch (sort) {
                    case "price-asc" -> Comparator.comparing(TourPackage::getPrice);
                    case "price-desc" -> Comparator.comparing(TourPackage::getPrice).reversed();
                    case "days" -> Comparator.comparing(TourPackage::getDuration);
                    default -> Comparator.comparing(TourPackage::getPackageId);
                })
                .toList();
        TreeSet<String> categories = new TreeSet<>();
        all.forEach(p -> { if (p.getCategory() != null && !p.getCategory().isBlank()) categories.add(p.getCategory()); });

        model.addAttribute("packages", shown);
        model.addAttribute("categories", categories);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("sort", sort);
        return "site/tours";
    }

    @GetMapping("/tours/{id}")
    public String tour(@PathVariable Integer id, Model model) {
        TourPackage pkg = packageRepository.findAllWithItineraries().stream()
                .filter(p -> p.getPackageId().equals(id) && p.isAvailable())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
        model.addAttribute("pkg", pkg);
        model.addAttribute("earliestDate", BookingService.earliestTravelDate());
        model.addAttribute("maxTravellers", BookingService.MAX_TRAVELLERS);
        model.addAttribute("related", available().stream().filter(p -> !p.getPackageId().equals(id)).limit(3).toList());
        return "site/tour";
    }

    private List<TourPackage> available() {
        return packageRepository.findAllWithItineraries().stream().filter(TourPackage::isAvailable).toList();
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }
}
