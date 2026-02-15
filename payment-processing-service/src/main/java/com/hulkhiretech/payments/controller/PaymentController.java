package com.hulkhiretech.payments.controller;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.pojo.CreateTxnRequest;
import com.hulkhiretech.payments.pojo.NotificationRequest;
import com.hulkhiretech.payments.pojo.TxnResponse;
import com.hulkhiretech.payments.pojo.InitiateTxnRequest;
import com.hulkhiretech.payments.service.interfaces.NotificationService;
import com.hulkhiretech.payments.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constant.V1_PAYMENTS)
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @PostMapping
    public TxnResponse createTxn(@RequestBody CreateTxnRequest createTxnRequest){
        log.info("Creating payment transaction || request: {}", createTxnRequest);

        TxnResponse response = paymentService.createTxn(createTxnRequest);
        log.info(" Response from service: {}", response);
        return response;
    }

    @PostMapping("/{txnReference}/initiate")
    public TxnResponse initiateTxn(@PathVariable String txnReference , @RequestBody InitiateTxnRequest initiateTxnRequest){
        log.info("Initiating payment transaction || id: {} | request: {}", txnReference , initiateTxnRequest);



        TxnResponse response = paymentService.initiateTxn(txnReference, initiateTxnRequest);
        log.info(" Response from service: {}", response);

        return response;
    }

    @PostMapping("/notifications")
    public String processNotification(
            @RequestBody NotificationRequest notificationRequest) {

        log.info("Processing payment notification"
                + "||notificationRequest:{}", notificationRequest);

        notificationService.processNotification(notificationRequest);

        return "";
    }

}
