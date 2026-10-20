package com.travel.web;

import com.travel.common.GlobalModelAdvice;
import com.travel.module3_billing.repository.PaymentSlipRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class StaffModelAdvice {

    private final PaymentSlipRepository slipRepository;

    public StaffModelAdvice(PaymentSlipRepository slipRepository) {
        this.slipRepository = slipRepository;
    }

    @ModelAttribute("requestPath")
    public String requestPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("pendingSlipCount")
    public Long pendingSlipCount() {
        boolean staff = GlobalModelAdvice.hasRole("ROLE_ADMIN") || GlobalModelAdvice.hasRole("ROLE_USER");
        return staff ? slipRepository.countByStatus("PENDING") : null;
    }
}
