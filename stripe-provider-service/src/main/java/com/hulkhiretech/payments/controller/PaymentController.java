package com.hulkhiretech.payments.controller;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.pojo.CreatePaymentRequest;
import com.hulkhiretech.payments.pojo.PaymentResponse;
import com.hulkhiretech.payments.interfaces.PaymentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constant.PAYMENTS)
@Slf4j
@RequiredArgsConstructor
public class PaymentController {


    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponse createPayment(@RequestBody CreatePaymentRequest createPaymentRequest) {
        log.info("Payment creation request received");

        log.info("CreatePaymentRequest: {}", createPaymentRequest);
        PaymentResponse response = paymentService.createPayment(createPaymentRequest);
        log.info("Payment created with response: {}", response);
        return response;
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable String id) {
        log.info("Get payment called in controller | id: {}", id);

        PaymentResponse response = paymentService.getPayment(id);
        return response;
    }

    @PostMapping("/{id}/expire")
    public PaymentResponse expirePayment(@PathVariable String id) {
        log.info("Expire payment called in controller | id: {}", id);

        PaymentResponse response = paymentService.expirePayment(id);
        return response;
    }

}
