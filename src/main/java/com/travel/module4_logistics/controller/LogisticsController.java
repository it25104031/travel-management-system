package com.travel.module4_logistics.controller;

import com.travel.module4_logistics.entity.Departure;
import com.travel.module4_logistics.service.LogisticsService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/logistics")
public class LogisticsController {
    private final LogisticsService logistics;
    private final com.travel.module5_packages.repository.TourPackageRepository packageRepository;

    public LogisticsController(LogisticsService logistics,
                               com.travel.module5_packages.repository.TourPackageRepository packageRepository) {
        this.logistics = logistics;
        this.packageRepository = packageRepository;
    }

    @GetMapping({"", "/"})
    public String home() { return "redirect:/logistics/departures"; }

    @GetMapping("/{page}")
    public String showPage(@PathVariable String page, Model model) {
        if (!java.util.Set.of("departures", "suppliers", "hotels", "transports", "guides", "assignments", "payments", "costs").contains(page))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        model.addAttribute("suppliers", logistics.listSuppliers());
        model.addAttribute("departures", logistics.listDepartures());
        model.addAttribute("hotels", logistics.listHotels());
        model.addAttribute("transports", logistics.listTransports());
        model.addAttribute("guides", logistics.listGuides());
        model.addAttribute("assignments", logistics.listAssignments());
        model.addAttribute("payments", logistics.listPayments());
        // Package names from Module 5, offered as suggestions when creating a departure
        model.addAttribute("packageNames", packageRepository.findAll().stream()
                .map(com.travel.module5_packages.entity.TourPackage::getPackageName).sorted().toList());
        model.addAttribute("page", page);
        if (page.equals("costs")) {
            Map<Integer, BigDecimal> costs = new LinkedHashMap<>();
            Map<Integer, BigDecimal> paid = new LinkedHashMap<>();
            for (Departure departure : logistics.listDepartures()) {
                costs.put(departure.getId(), logistics.costForDeparture(departure.getId()));
                paid.put(departure.getId(), logistics.paidForDeparture(departure.getId()));
            }
            model.addAttribute("costs", costs);
            model.addAttribute("paid", paid);
        }
        return "module4_logistics/" + page;
    }

    @PostMapping("/departures")
    public String addDeparture(@RequestParam String reference, @RequestParam String packageName,
                               @RequestParam String destination, @RequestParam LocalDate startDate,
                               @RequestParam LocalDate endDate, RedirectAttributes message) {
        return save(message, () -> logistics.addDeparture(reference, packageName, destination, startDate, endDate), "Departure saved", "departures");
    }

    @PostMapping("/suppliers")
    public String addSupplier(@RequestParam String name, @RequestParam String type,
                              @RequestParam(required = false) String phone,
                              @RequestParam LocalDate contractStart, @RequestParam LocalDate contractEnd,
                              @RequestParam(required = false) Integer roomCapacity, RedirectAttributes message) {
        return save(message, () -> logistics.addSupplier(name, type, phone, contractStart, contractEnd, roomCapacity), "Supplier saved", "suppliers");
    }

    @GetMapping("/suppliers/{id}/edit")
    public String editSupplier(@PathVariable Integer id, Model model) {
        if (!model.containsAttribute("editingSupplier"))
            model.addAttribute("editingSupplier", logistics.getSupplier(id));
        return showPage("suppliers", model);
    }

    @PostMapping("/suppliers/{id}/edit")
    public String updateSupplier(@PathVariable Integer id,
                                 @RequestParam String name, @RequestParam String type,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam LocalDate contractStart, @RequestParam LocalDate contractEnd,
                                 @RequestParam(required = false) Integer roomCapacity,
                                 Model model, RedirectAttributes message) {
        try {
            logistics.updateSupplier(id, name, type, phone, contractStart, contractEnd, roomCapacity);
            message.addFlashAttribute("notice", "Supplier updated");
            return "redirect:/logistics/suppliers";
        } catch (ResponseStatusException error) {
            Map<String, Object> submitted = new LinkedHashMap<>();
            submitted.put("id", id);
            submitted.put("name", name);
            submitted.put("type", type);
            submitted.put("contactPhone", phone);
            submitted.put("contractStart", contractStart);
            submitted.put("contractEnd", contractEnd);
            submitted.put("roomCapacity", roomCapacity);
            model.addAttribute("editingSupplier", submitted);
            model.addAttribute("error", error.getReason());
            return showPage("suppliers", model);
        }
    }

    @PostMapping("/hotels")
    public String bookHotel(@RequestParam Integer supplierId, @RequestParam Integer departureId,
                            @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut,
                            @RequestParam Integer rooms, @RequestParam BigDecimal cost, RedirectAttributes message) {
        return save(message, () -> logistics.bookHotel(supplierId, departureId, checkIn, checkOut, rooms, cost), "Rooms allocated", "hotels");
    }

    @PostMapping("/transports")
    public String addTransport(@RequestParam Integer supplierId, @RequestParam Integer departureId,
                               @RequestParam String mode, @RequestParam String reference,
                               @RequestParam LocalDate serviceDate, @RequestParam Integer seats,
                               @RequestParam BigDecimal cost, RedirectAttributes message) {
        return save(message, () -> logistics.addTransport(supplierId, departureId, mode, reference, serviceDate, seats, cost), "Transport allocated", "transports");
    }

    @PostMapping("/guides")
    public String addGuide(@RequestParam Integer supplierId, @RequestParam String name,
                           @RequestParam String role, @RequestParam(required = false) String phone,
                           @RequestParam BigDecimal dailyRate, RedirectAttributes message) {
        return save(message, () -> logistics.addGuide(supplierId, name, role, phone, dailyRate), "Guide or driver saved", "guides");
    }

    @PostMapping("/assignments")
    public String assignGuide(@RequestParam Integer guideId, @RequestParam Integer departureId,
                              @RequestParam LocalDate startDate, @RequestParam LocalDate endDate,
                              RedirectAttributes message) {
        return save(message, () -> logistics.assignGuide(guideId, departureId, startDate, endDate), "Staff assigned", "assignments");
    }

    @PostMapping("/payments")
    public String recordPayment(@RequestParam Integer supplierId, @RequestParam Integer departureId,
                                @RequestParam LocalDate paymentDate, @RequestParam BigDecimal amount,
                                @RequestParam(required = false) String reference, RedirectAttributes message) {
        return save(message, () -> logistics.recordPayment(supplierId, departureId, paymentDate, amount, reference), "Supplier payment recorded", "payments");
    }

    @GetMapping("/{page}/{id}/edit")
    public String editRecord(@PathVariable String page, @PathVariable Integer id, Model model) {
        model.addAttribute("editing", logistics.editValues(page, id));
        return showPage(page, model);
    }

    @PostMapping("/{page}/{id}/edit")
    public String updateRecord(@PathVariable String page, @PathVariable Integer id,
                               @RequestParam Map<String, String> values, Model model,
                               RedirectAttributes message) {
        try {
            logistics.updateRecord(page, id, values);
            message.addFlashAttribute("notice", "Record updated");
            return "redirect:/logistics/" + page;
        } catch (ResponseStatusException error) {
            Map<String, Object> submitted = new LinkedHashMap<>(values);
            submitted.put("id", id);
            model.addAttribute("editing", submitted);
            model.addAttribute("error", error.getReason());
            return showPage(page, model);
        }
    }

    @PostMapping("/{page}/delete")
    public String deleteRecords(@PathVariable String page,
                                 @RequestParam(required = false) java.util.List<Integer> ids,
                                 RedirectAttributes message) {
        if (!java.util.Set.of("departures", "suppliers", "hotels", "transports", "guides", "assignments", "payments").contains(page))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        try {
            logistics.deleteRecords(page, ids);
            message.addFlashAttribute("notice", "Selected records deleted");
        } catch (ResponseStatusException error) {
            message.addFlashAttribute("error", error.getReason());
        } catch (org.springframework.dao.DataIntegrityViolationException error) {
            message.addFlashAttribute("error", "Nothing deleted. A selected record is linked to another record. Refresh and try again");
        }
        return "redirect:/logistics/" + page;
    }

    private String save(RedirectAttributes message, Runnable work, String success, String page) {
        try {
            work.run();
            message.addFlashAttribute("notice", success);
        } catch (ResponseStatusException error) {
            message.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/logistics/" + page;
    }
}
