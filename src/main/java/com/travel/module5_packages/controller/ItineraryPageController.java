package com.travel.module5_packages.controller;

import com.travel.module5_packages.entity.Itinerary;
import com.travel.module5_packages.entity.TourPackage;
import com.travel.module5_packages.repository.ItineraryRepository;
import com.travel.module5_packages.repository.TourPackageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ItineraryPageController {

    private final ItineraryRepository itineraryRepository;
    private final TourPackageRepository packageRepository;

    public ItineraryPageController(ItineraryRepository itineraryRepository,
                                   TourPackageRepository packageRepository) {
        this.itineraryRepository = itineraryRepository;
        this.packageRepository = packageRepository;
    }

    @GetMapping("/itineraries/{packageId}")
    public String page(@PathVariable Integer packageId, Model model) {
        loadItineraryPage(packageId, model);
        return "module5_packages/itineraries";
    }

    @PostMapping("/itineraries/add")
    public String add(
            @RequestParam(required = false) Integer packageId,
            @RequestParam(required = false) Integer dayNumber,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String activities,
            @RequestParam(required = false) String accommodation,
            @RequestParam(required = false) String transport,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (packageId == null || !packageRepository.existsById(packageId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid tour package.");
            return "redirect:/packages";
        }
        TourPackage tourPackage = packageRepository.findById(packageId).orElseThrow();
        String validationMessage = validateItinerary(tourPackage, dayNumber, destination, activities,
                accommodation, transport);
        if (validationMessage != null) {
            model.addAttribute("errorMessage", validationMessage);
            loadItineraryPage(packageId, model);
            return "module5_packages/itineraries";
        }
        if (itineraryRepository.existsByTourPackage_PackageIdAndDayNumber(packageId, dayNumber)) {
            model.addAttribute("errorMessage", "Day " + dayNumber + " already exists for this package.");
            loadItineraryPage(packageId, model);
            return "module5_packages/itineraries";
        }

        Itinerary itinerary = new Itinerary();
        itinerary.setTourPackage(tourPackage);
        applyItineraryFields(itinerary, dayNumber, destination, activities, accommodation, transport);
        try {
            itineraryRepository.save(itinerary);
        } catch (DataIntegrityViolationException exception) {
            model.addAttribute("errorMessage", "That itinerary day already exists. Refresh the page and try again.");
            loadItineraryPage(packageId, model);
            return "module5_packages/itineraries";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Itinerary day added successfully.");
        return "redirect:/itineraries/" + packageId;
    }

    @GetMapping("/itineraries/edit/{id}")
    public String showEditPage(@PathVariable Integer id, Model model) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary not found."));
        model.addAttribute("itinerary", itinerary);
        model.addAttribute("tourPackage", itinerary.getTourPackage());
        return "module5_packages/edit-itinerary";
    }

    @PostMapping("/itineraries/update/{id}")
    public String update(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer dayNumber,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String activities,
            @RequestParam(required = false) String accommodation,
            @RequestParam(required = false) String transport,
            Model model,
            RedirectAttributes redirectAttributes) {

        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary not found."));
        TourPackage tourPackage = itinerary.getTourPackage();
        Integer packageId = tourPackage.getPackageId();

        String validationMessage = validateItinerary(tourPackage, dayNumber, destination, activities,
                accommodation, transport);
        if (validationMessage != null) {
            model.addAttribute("errorMessage", validationMessage);
            model.addAttribute("itinerary", itinerary);
            model.addAttribute("tourPackage", tourPackage);
            return "module5_packages/edit-itinerary";
        }
        boolean duplicate = itineraryRepository
                .existsByTourPackage_PackageIdAndDayNumber(packageId, dayNumber);
        if (duplicate && !dayNumber.equals(itinerary.getDayNumber())) {
            model.addAttribute("errorMessage", "Day " + dayNumber + " already exists for this package.");
            model.addAttribute("itinerary", itinerary);
            model.addAttribute("tourPackage", tourPackage);
            return "module5_packages/edit-itinerary";
        }

        applyItineraryFields(itinerary, dayNumber, destination, activities, accommodation, transport);
        try {
            itineraryRepository.save(itinerary);
        } catch (DataIntegrityViolationException exception) {
            model.addAttribute("errorMessage", "That itinerary day already exists. Refresh the page and try again.");
            model.addAttribute("itinerary", itinerary);
            model.addAttribute("tourPackage", tourPackage);
            return "module5_packages/edit-itinerary";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Itinerary day updated successfully.");
        return "redirect:/itineraries/" + packageId;
    }

    @PostMapping("/itineraries/delete/{id}")
    public String delete(@PathVariable Integer id,
                         @RequestParam Integer packageId,
                         RedirectAttributes redirectAttributes) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary not found."));
        Integer actualPackageId = itinerary.getTourPackage().getPackageId();
        if (!actualPackageId.equals(packageId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "The itinerary does not belong to that package.");
            return "redirect:/itineraries/" + actualPackageId;
        }
        itineraryRepository.delete(itinerary);
        redirectAttributes.addFlashAttribute("successMessage", "Itinerary day deleted successfully.");
        return "redirect:/itineraries/" + actualPackageId;
    }

    private String validateItinerary(TourPackage tourPackage, Integer dayNumber,
                                     String destination, String activities,
                                     String accommodation, String transport) {
        if (dayNumber == null || dayNumber < 1 || dayNumber > tourPackage.getDuration()) {
            return "Day number must be between 1 and " + tourPackage.getDuration() + ".";
        }
        if (destination == null || destination.isBlank()) {
            return "Destination is required.";
        }
        if (destination.trim().length() > 100) {
            return "Destination must be 100 characters or fewer.";
        }
        if (activities == null || activities.isBlank()) {
            return "Activities are required.";
        }
        if (activities.length() > 1000) {
            return "Activities must be 1,000 characters or fewer.";
        }
        if (accommodation != null && accommodation.length() > 255) {
            return "Accommodation must be 255 characters or fewer.";
        }
        if (transport != null && transport.length() > 255) {
            return "Transport must be 255 characters or fewer.";
        }
        return null;
    }

    private void applyItineraryFields(Itinerary itinerary, Integer dayNumber,
                                      String destination, String activities,
                                      String accommodation, String transport) {
        itinerary.setDayNumber(dayNumber);
        itinerary.setDestination(destination.trim());
        itinerary.setActivities(activities.trim());
        itinerary.setAccommodation(blankToNull(accommodation));
        itinerary.setTransport(blankToNull(transport));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void loadItineraryPage(Integer packageId, Model model) {
        TourPackage tourPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Tour package not found."));
        model.addAttribute("tourPackage", tourPackage);
        model.addAttribute("itineraries",
                itineraryRepository.findByTourPackage_PackageIdOrderByDayNumberAsc(packageId));
    }
}
