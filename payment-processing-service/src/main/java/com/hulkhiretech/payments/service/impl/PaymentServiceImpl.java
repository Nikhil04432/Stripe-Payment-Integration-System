package com.hulkhiretech.payments.service.impl;

import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.http.HttpServiceEngine;
import com.hulkhiretech.payments.pojo.CreateTxnRequest;
import com.hulkhiretech.payments.pojo.TxnResponse;
import com.hulkhiretech.payments.pojo.InitiateTxnRequest;
import com.hulkhiretech.payments.service.helper.SPCreatePaymentHelper;
import com.hulkhiretech.payments.service.interfaces.PaymentService;
import com.hulkhiretech.payments.service.interfaces.PaymentStatusService;
import com.hulkhiretech.payments.stripeprovider.StripeProviderPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentStatusService paymentStatusService;
    private final ModelMapper modelMapper;
    private final TransactionDao transactionDao;
    private final HttpServiceEngine httpServiceEngine;
    private final SPCreatePaymentHelper spCreatePaymentHelper;

    @Override
    public TxnResponse createTxn(CreateTxnRequest createTxnRequest) {
        log.info("Creating payment transaction request: {}", createTxnRequest);
        //TODO

        TransactionDTO txnDto = modelMapper.map(createTxnRequest, TransactionDTO.class);
        log.info(" Mapped TransactionDTO txnDto : {}", txnDto);
  

        String txnStatus = TransactionStatusEnum.CREATED.name();
        // generate UUID for txnReference
        String txnReference = UUID.randomUUID().toString();

        txnDto.setTxnStatus(txnStatus);
        txnDto.setTxnReference(txnReference);

        TransactionDTO response = paymentStatusService.processStatus(txnDto);
        log.info("Processed transaction status with ID: {}", txnStatus);

        TxnResponse createTxnResponse = new TxnResponse();
        createTxnResponse.setTxnStatusId(response.getTxnStatus());
        createTxnResponse.setTxnReference(response.getTxnReference());

        log.info("Created CreateTxnResponse: {}", createTxnResponse);

        return createTxnResponse;
    }

    @Override
    public TxnResponse initiateTxn(String txnReference, InitiateTxnRequest initiateTxnRequest) {
        log.info("Initiating payment transaction || id:{} | initiateTxnRequest : {}", txnReference, initiateTxnRequest);
        TransactionEntity txnEntity = transactionDao.getTransactionByTxnReference(txnReference);
        log.info("Fetched TransactionEntity: {}", txnEntity);

        TransactionDTO txnDto = modelMapper.map(txnEntity, TransactionDTO.class);
        txnDto.setTxnStatus(TransactionStatusEnum.INITIATED.name());

        txnDto = paymentStatusService.processStatus(txnDto);
        // TODO: Make rest call to Stripe to initiate payment

        HttpRequest httpRequest =  spCreatePaymentHelper.prepareHttpRequest(initiateTxnRequest);
        log.info("HttpRequest prepared from helper: {}", httpRequest);

        ResponseEntity<String> response = httpServiceEngine.makeHttpCall(httpRequest);
        log.info("Response from httpCall method : {}", response);
        // Process the response from Stripe
        StripeProviderPaymentResponse successResponse = spCreatePaymentHelper.processResponse(response);
        log.info("StripeProviderPaymentResponse from helper: {}", successResponse);

        // Update the transaction status to PENDING after initiating payment
        txnDto.setTxnStatus(TransactionStatusEnum.PENDING.name());
        txnDto.setProviderReference(successResponse.getId());

        txnDto = paymentStatusService.processStatus(txnDto);
        log.info("Updated transaction status to PENDING: {}", txnDto);

        TxnResponse txnResponse = new TxnResponse();
        txnResponse.setTxnReference(txnDto.getTxnReference());
        txnResponse.setTxnStatusId(txnDto.getTxnStatus());
        txnResponse.setRedirectUrl(successResponse.getUrl());

        return txnResponse;
    }

}
