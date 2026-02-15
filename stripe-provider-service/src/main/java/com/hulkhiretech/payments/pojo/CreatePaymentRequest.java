package com.hulkhiretech.payments.pojo;

import java.util.List;
import lombok.Data;
@Data
public class CreatePaymentRequest {
    // this class is for accpeting the data from the merchant
    private String successUrl;
    private String cancelUrl;

    private List<LineItem> lineItems;
}
