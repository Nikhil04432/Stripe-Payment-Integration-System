package com.hulkhiretech.payments.pojo;

import com.hulkhiretech.payments.stripeprovider.LineItem;
import lombok.Data;

import java.util.List;

@Data
public class InitiateTxnRequest {
    //TODO add th necessary fields later on
    private String successUrl;
    private String cancelUrl;

    private List<LineItem> lineItems;
}
