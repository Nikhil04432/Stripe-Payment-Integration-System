package com.hulkhiretech.payments.stripeprovider;

import lombok.Data;

@Data
public class StripeProviderPaymentResponse {
    private String id;
    private String url;
    private String sessionStatus;
    private String paymentStatus;
}
