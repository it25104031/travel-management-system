package com.travel.module4_logistics.controller;

import com.travel.module4_logistics.service.LogisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsApiController {
    private final LogisticsService logistics;

    public LogisticsApiController(LogisticsService logistics) {
        this.logistics = logistics;
    }

    @GetMapping("/departures/{departureId}/cost")
    public CostView departureCost(@PathVariable Integer departureId) {
        BigDecimal total = logistics.costForDeparture(departureId);
        BigDecimal paid = logistics.paidForDeparture(departureId);
        return new CostView(departureId, total, paid, total.subtract(paid));
    }

    public record CostView(Integer departureId, BigDecimal supplierCost, BigDecimal paid, BigDecimal outstanding) { }
}
