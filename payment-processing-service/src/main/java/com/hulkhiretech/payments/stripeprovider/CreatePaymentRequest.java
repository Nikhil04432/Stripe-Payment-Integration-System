package com.hulkhiretech.payments.stripeprovider;

import lombok.Data;

import java.util.List;
@Data
public class CreatePaymentRequest {
    // this class is for accpeting the data from the merchant
    private String successUrl;
    private String cancelUrl;

    private List<LineItem> lineItems;
}
