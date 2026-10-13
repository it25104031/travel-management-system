package com.travel.module5_packages.controller;

import com.travel.module5_packages.entity.TourPackage;
import com.travel.module5_packages.repository.ItineraryRepository;
import com.travel.module5_packages.repository.TourBookingRepository;
import com.travel.module5_packages.repository.TourPackageRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class TourPackagePageController {

    private final TourPackageRepository repository;
    private final ItineraryRepository itineraryRepository;
    private final TourBookingRepository bookingRepository;

    public TourPackagePageController(
            TourPackageRepository repository,
            ItineraryRepository itineraryRepository,
            TourBookingRepository bookingRepository) {
        this.repository = repository;
        this.itineraryRepository = itineraryRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/packages")
    public String packages(Model model) {
        model.addAttribute("packages", repository.findAllWithItineraries());
        return "module5_packages/packages";
    }

    @PostMapping("/packages/add")
    public String add(
            @RequestParam(required = false) String packageName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer duration,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttributes) {

        String validationMessage = validatePackage(packageName, description, duration, price, status);
        if (validationMessage == null) validationMessage = validateWebsiteFields(destination, category, imageUrl);
        if (validationMessage != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationMessage);
            return "redirect:/packages";
        }

        TourPackage tourPackage = new TourPackage();
        applyPackageFields(tourPackage, packageName, description, duration, price, status);
        applyWebsiteFields(tourPackage, destination, category, imageUrl);
        repository.save(tourPackage);
        redirectAttributes.addFlashAttribute("successMessage", "Tour package created successfully.");
        return "redirect:/packages";
    }

    @GetMapping("/packages/edit/{id}")
    public String showEditPage(@PathVariable Integer id, Model model) {
        TourPackage tourPackage = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tour package not found."));
        model.addAttribute("tourPackage", tourPackage);
        return "module5_packages/edit-package";
    }

    @PostMapping("/packages/update/{id}")
    public String update(
            @PathVariable Integer id,
            @RequestParam(required = false) String packageName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer duration,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttributes) {

        TourPackage tourPackage = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tour package not found."));

        String validationMessage = validatePackage(packageName, description, duration, price, status);
        if (validationMessage == null) validationMessage = validateWebsiteFields(destination, category, imageUrl);
        if (validationMessage == null) {
            Integer highestPlannedDay = itineraryRepository.findMaxDayNumberByPackageId(id);
            if (highestPlannedDay != null && duration < highestPlannedDay) {
                validationMessage = "Duration cannot be shorter than the highest planned itinerary day (Day "
                        + highestPlannedDay + "). Update the itinerary first.";
            }
        }
        if (validationMessage != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationMessage);
            return "redirect:/packages/edit/" + id;
        }

        applyPackageFields(tourPackage, packageName, description, duration, price, status);
        applyWebsiteFields(tourPackage, destination, category, imageUrl);
        repository.save(tourPackage);
        redirectAttributes.addFlashAttribute("successMessage", "Tour package updated successfully.");
        return "redirect:/packages";
    }

    @PostMapping("/packages/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        if (!repository.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "That tour package no longer exists.");
            return "redirect:/packages";
        }
        if (bookingRepository.existsByTourPackage_PackageId(id)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This package has customer bookings, so it can't be deleted. Set it to UNAVAILABLE to hide it from the website.");
            return "redirect:/packages";
        }
        repository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Tour package and its itinerary were deleted.");
        return "redirect:/packages";
    }

    private String validatePackage(String packageName, String description,
                                   Integer duration, BigDecimal price, String status) {
        if (packageName == null || packageName.isBlank()) {
            return "Package name is required.";
        }
        if (packageName.trim().length() > 150) {
            return "Package name must be 150 characters or fewer.";
        }
        if (description == null || description.isBlank()) {
            return "Description is required.";
        }
        if (description.length() > 1000) {
            return "Description must be 1,000 characters or fewer.";
        }
        if (duration == null || duration < 1) {
            return "Duration must be at least one day.";
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return "Price must be zero or greater.";
        }
        if (!"AVAILABLE".equals(status) && !"UNAVAILABLE".equals(status)) {
            return "Choose either AVAILABLE or UNAVAILABLE for status.";
        }
        return null;
    }

    private void applyPackageFields(TourPackage tourPackage, String packageName,
                                    String description, Integer duration,
                                    BigDecimal price, String status) {
        tourPackage.setPackageName(packageName.trim());
        tourPackage.setDescription(description.trim());
        tourPackage.setDuration(duration);
        tourPackage.setPrice(price);
        tourPackage.setStatus(status);
    }

    private String validateWebsiteFields(String destination, String category, String imageUrl) {
        if (destination != null && destination.trim().length() > 100) {
            return "Destination must be 100 characters or fewer.";
        }
        if (category != null && category.trim().length() > 40) {
            return "Category must be 40 characters or fewer.";
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            String url = imageUrl.trim();
            if (url.length() > 500) return "Image URL must be 500 characters or fewer.";
            if (!url.matches("[A-Za-z0-9._~:/?#\\[\\]@!$&*+,;=%-]+")) return "Image URL contains characters that aren't allowed.";
            if (!url.startsWith("https://") && !url.startsWith("http://") && !url.startsWith("/img/")) {
                return "Image URL must start with https:// (or /img/ for images inside the project).";
            }
        }
        return null;
    }

    private void applyWebsiteFields(TourPackage tourPackage, String destination, String category, String imageUrl) {
        tourPackage.setDestination(blankToNull(destination));
        tourPackage.setCategory(blankToNull(category));
        tourPackage.setImageUrl(blankToNull(imageUrl));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
