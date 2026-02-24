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

    public PaymentResponse getPaymentBySessionId(String sessionId) {

        return restClient.get()
                .uri(paymentServiceBaseUrl + "/payments/{id}", sessionId)
                .headers(prepareHttpRequest())
                .retrieve()
                .body(PaymentResponse.class);
    }

    private Consumer<HttpHeaders> prepareHttpRequest() {
        return headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
        };
    }
}