package com.hulkhiretech.payments.http;

import com.hulkhiretech.payments.pojo.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Consumer;

@Component
public class PaymentClient {

    private final RestClient restClient;

    @Value("${payment.service.base-url}")
    private String paymentServiceBaseUrl;

    public PaymentClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    // -----------------------------------------------------------------------
    // GET /payments/{sessionId}
    // Used by ReconService to check if Stripe payment is paid/unpaid/expired
    // -----------------------------------------------------------------------
    public PaymentResponse getPaymentBySessionId(String sessionId) {
        return restClient.get()
                .uri(paymentServiceBaseUrl + "/payments/{id}", sessionId)
                .headers(prepareJsonHeaders())
                .retrieve()
                .body(PaymentResponse.class);
    }

    // -----------------------------------------------------------------------
    // POST /payments/{sessionId}/expire
    // Used by ReconService when retryCount >= MAX_RETRY_COUNT.
    // This forcefully closes the Stripe hosted payment page so the customer
    // cannot complete the payment anymore.
    // -----------------------------------------------------------------------
    public PaymentResponse expirePaymentBySessionId(String sessionId) {
        return restClient.post()
                .uri(paymentServiceBaseUrl + "/payments/{id}/expire", sessionId)
                .headers(prepareJsonHeaders())
                .retrieve()
                .body(PaymentResponse.class);
    }

    // -----------------------------------------------------------------------
    // Common headers for JSON requests
    // -----------------------------------------------------------------------
    private Consumer<HttpHeaders> prepareJsonHeaders() {
        return headers -> headers.setContentType(MediaType.APPLICATION_JSON);
    }
}