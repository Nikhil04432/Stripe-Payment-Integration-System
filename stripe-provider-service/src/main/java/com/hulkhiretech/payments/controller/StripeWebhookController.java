package com.hulkhiretech.payments.controller;

import com.hulkhiretech.payments.interfaces.StripeWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/stripe/webhook")
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookController {
    // This controller will handle the webhook events from stripe
    private final StripeWebhookService stripeWebhookService;

    @PostMapping
    public String handleWebhook(
            @RequestHeader ("Stripe-Signature") String sigHeader,
            @RequestBody String payload) {
        log.info("Webhook event received || sigHeader :{} | payload: {}", sigHeader, payload);

        String response = stripeWebhookService.handleWebhook(sigHeader,payload);
        log.info("Webhook event processed || response: {}", response);


        return "response";
    }
}
