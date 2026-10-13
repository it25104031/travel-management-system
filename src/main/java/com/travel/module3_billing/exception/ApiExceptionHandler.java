package com.travel.module3_billing.exception;

import com.travel.module3_billing.api.ApiBillingController;
import com.travel.module3_billing.api.ApiSlipController;
import com.travel.module3_billing.controller.InvoiceController;
import com.travel.module3_billing.controller.PaymentController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Turns errors from the REST API (/api/...) into clean JSON responses
 * like {"error": "..."} with a proper status code, instead of a 500.
 */
@RestControllerAdvice(assignableTypes = {InvoiceController.class, PaymentController.class,
        ApiBillingController.class, ApiSlipController.class})
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissing(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Missing field: " + e.getParameterName()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleUnreadable(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request data."));
    }
}
