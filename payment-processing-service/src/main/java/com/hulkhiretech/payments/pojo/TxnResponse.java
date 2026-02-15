package com.hulkhiretech.payments.pojo;

import lombok.Data;

@Data
public class TxnResponse {
    private String txnStatusId;
    private String txnReference;
    private String redirectUrl;

}
