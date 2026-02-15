package com.hulkhiretech.payments.util;

import com.hulkhiretech.payments.pojo.PaymentResponse;
import com.hulkhiretech.payments.stripe.StripeResponse;

public class StripeResponseUtil {
    private StripeResponseUtil() {
        // private constructor to prevent instantiation
    }

    public static PaymentResponse preparePaymentRespose(StripeResponse stripeResponse) {
        PaymentResponse response = new PaymentResponse();
        response.setId(stripeResponse.getId());
        response.setUrl(stripeResponse.getUrl());
        response.setSessionStatus(stripeResponse.getStatus());
        response.setPaymentStatus(stripeResponse.getPaymentStatus());
        return response;
    }

}
