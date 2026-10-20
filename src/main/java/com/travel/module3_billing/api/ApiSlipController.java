package com.travel.module3_billing.api;

import com.travel.module3_billing.api.ApiDtos.RemarkRequest;
import com.travel.module3_billing.api.ApiDtos.SlipDto;
import com.travel.module3_billing.config.CurrentStaff;
import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.exception.NotFoundException;
import com.travel.module3_billing.repository.InvoiceRepository;
import com.travel.module3_billing.service.PaymentSlipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ApiSlipController {

    private final PaymentSlipService paymentSlipService;
    private final InvoiceRepository invoiceRepository;
    private final CurrentStaff currentStaff;

    public ApiSlipController(PaymentSlipService paymentSlipService, InvoiceRepository invoiceRepository,
                             CurrentStaff currentStaff) {
        this.paymentSlipService = paymentSlipService;
        this.invoiceRepository = invoiceRepository;
        this.currentStaff = currentStaff;
    }

    @GetMapping("/api/admin/slips")
    public List<SlipDto> allSlips(@RequestParam(required = false, defaultValue = "ALL") String status) {
        return paymentSlipService.getAllPaymentSlips(status).stream()
                .map(SlipDto::of)
                .toList();
    }

    @PostMapping("/api/admin/slips/{id}/approve")
    public SlipDto approve(@PathVariable Integer id, @RequestBody(required = false) RemarkRequest body) {
        paymentSlipService.approveSlip(id, body != null ? body.adminRemark() : null);
        return SlipDto.of(getSlip(id));
    }

    @PostMapping("/api/admin/slips/{id}/reject")
    public SlipDto reject(@PathVariable Integer id, @RequestBody(required = false) RemarkRequest body) {
        paymentSlipService.rejectSlip(id, body != null ? body.adminRemark() : null);
        return SlipDto.of(getSlip(id));
    }

    private com.travel.module3_billing.entity.PaymentSlip getSlip(Integer id) {
        return paymentSlipService.getPaymentSlipById(id)
                .orElseThrow(() -> new NotFoundException("Payment slip #" + id + " was not found."));
    }

}
