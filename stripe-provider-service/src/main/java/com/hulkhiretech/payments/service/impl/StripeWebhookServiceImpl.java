package com.hulkhiretech.payments.service.impl;

import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.StripeProviderException;
import com.hulkhiretech.payments.interfaces.StripeWebhookService;
import com.hulkhiretech.payments.service.ProcessStripeEventAsync;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private final ProcessStripeEventAsync processStripeEventAsync;
    @Value("${stripe.webhook.secret}")
    private String webPointSecret;

    @Override
    public String handleWebhook(String sigHeader, String payload) {
        log.info("Webhook event received || payload: {}", payload);

        Event event = checkSignValid(sigHeader, payload);

        log.info("HmacSHA 256 Signature is valid, Now Processing event || event type: {}", event.getType());
        // Handle the event

        processStripeEventAsync.processEvent(event);
        log.info("Event processing is done || event id: {}", event.getId());

        return "Webhook handled successfully";
    }

    private Event checkSignValid(String sigHeader, String payload) {
        Event event = null;
        try{
            event = Webhook.constructEvent(
                    payload, sigHeader, webPointSecret
            );

            // log event id and event type only
            log.info("Stripe Signature is valid for event with || event id: {} | event type: {}", event.getId(), event.getType());
            return event;
        }catch (Exception e){
            log.error("Error while constructing event from webhook payload || error: {}", e.getMessage());
            throw new StripeProviderException(
                    ErrorCodeEnum.INVALID_STRIPE_SIGNATURE.getErrorCode(),
                    ErrorCodeEnum.INVALID_STRIPE_SIGNATURE.getErrorMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
