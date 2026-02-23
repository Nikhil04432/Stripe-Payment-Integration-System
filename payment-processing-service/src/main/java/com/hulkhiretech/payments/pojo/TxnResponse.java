package com.hulkhiretech.payments.pojo;

import lombok.Data;

@Data
public class TxnResponse {
    private String txnStatus;
    private String txnReference;
    private String redirectUrl;

}
