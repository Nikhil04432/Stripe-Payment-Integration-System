package com.hulkhiretech.payments.service.interfaces;

import com.hulkhiretech.payments.pojo.CreateTxnRequest;
import com.hulkhiretech.payments.pojo.TxnResponse;
import com.hulkhiretech.payments.pojo.InitiateTxnRequest;

public interface PaymentService {
    public TxnResponse createTxn(CreateTxnRequest createTxnRequest);
    public TxnResponse initiateTxn(String id, InitiateTxnRequest initiateTxnRequest);
}
