package com.hulkhiretech.payments.interfaces;


public interface StripeWebhookService {

    public String handleWebhook(String sigHeader, String payload);
}
