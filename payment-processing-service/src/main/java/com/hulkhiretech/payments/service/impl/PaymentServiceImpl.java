package com.hulkhiretech.payments.service.impl;

import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.exception.ProcessingException;
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

import java.util.Optional;
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

        // ---------------------------------------------------------------
        // IDEMPOTENCY CHECK
        //
        // WHAT: Before doing anything, check if this merchantTransactionReference
        // already exists in our DB.
        //
        // WHY: Merchant might send the same request twice due to:
        //   - Network timeout on their side (they retry thinking it failed)
        //   - Double-click on Pay button
        //   - Their own retry logic
        //
        // Without this check: 2 transactions created for same order = overcharge risk
        // With this check: 2nd request gets same response as 1st = safe
        //
        // HOW: merchantTransactionReference is merchant's own unique order ID.
        // Same order always has same reference. Perfect idempotency key.
        // ---------------------------------------------------------------
        TxnResponse existingTxnDto = idempotencyCheck(createTxnRequest);
        if (existingTxnDto != null) return existingTxnDto;

        // ---------------------------------------------------------------
        // FRESH REQUEST - no duplicate found, proceed normally
        // ---------------------------------------------------------------
        log.info("Fresh request. Proceeding with new transaction creation.");

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
        createTxnResponse.setTxnStatus(response.getTxnStatus());
        createTxnResponse.setTxnReference(response.getTxnReference());

        log.info("Created CreateTxnResponse: {}", createTxnResponse);

        return createTxnResponse;
    }

    private TxnResponse idempotencyCheck(CreateTxnRequest createTxnRequest) {
        Optional<TransactionEntity> existingTxn = transactionDao
                .findByMerchantTransactionReference(
                        createTxnRequest.getMerchantTransactionReference());

        if (existingTxn.isPresent()) {
            log.info("Idempotent request detected for merchantTransactionReference: {}. "
                            + "Returning existing response.",
                    createTxnRequest.getMerchantTransactionReference());

            // Convert existing entity to DTO and return same response
            // Merchant gets consistent result - they won't know it was duplicate
            TransactionDTO existingTxnDto = modelMapper.map(
                    existingTxn.get(), TransactionDTO.class);

            return buildTxnResponse(existingTxnDto);
        }
        return null;
    }

    // ---------------------------------------------------------------
    // HELPER: builds TxnResponse from TransactionDTO
    // Extracted to avoid code duplication between fresh and duplicate paths.
    // ---------------------------------------------------------------
    private TxnResponse buildTxnResponse(TransactionDTO txnDto) {
        TxnResponse response = new TxnResponse();
        response.setTxnStatus(txnDto.getTxnStatus());
        response.setTxnReference(txnDto.getTxnReference());
        return response;
    }

    @Override
    public TxnResponse initiateTxn(String txnReference, InitiateTxnRequest initiateTxnRequest) {
        log.info("Initiating payment transaction||id:{}|initiateTxnRequest:{}",
                txnReference, initiateTxnRequest);

        // update DB as INITIATED
        TransactionEntity txnEntity = transactionDao
                .getTransactionByTxnReference(txnReference);
        TransactionDTO txnDto = modelMapper.map(txnEntity, TransactionDTO.class);
        log.info("Mapped txnDto: {}", txnDto);

        txnDto.setTxnStatus(TransactionStatusEnum.INITIATED.name());
        txnDto = paymentStatusService.processStatus(txnDto);
        log.info("Updated txnDto to INITIATED: {}", txnDto);

        StripeProviderPaymentResponse successResponse = null;

        // before we were just making status as pending in db but not think of one edge case that if
        // there is any exception while making rest call to provider then we can update the status as failed
        // in db and log the error message and error code for future reference. so added the try catch block
        // to handle that scenario.

        try {
            // Make Rest Http API call to stripe-provider-service for create-payment api
            HttpRequest httpRequest = spCreatePaymentHelper
                    .prepareHttpRequest(initiateTxnRequest);
            log.info("Prepared HttpRequest for stripe-provider-service: {}", httpRequest);

            ResponseEntity<String> response = httpServiceEngine.makeHttpCall(httpRequest);
            log.info("Response from stripe-provider-service: {}", response);

            successResponse = spCreatePaymentHelper.processResponse(response);
            log.info("Processed StripeProviderPaymentResponse: {}", successResponse);

        } catch (ProcessingException e) {
            log.error("Error during initiating transaction: {}", e.getMessage());
            // Handle exception, possibly update txnDto to FAILED status
            txnDto.setTxnStatus(TransactionStatusEnum.FAILED.name());
            txnDto.setErrorCode(e.getErrorCode());
            txnDto.setErrorMessage(e.getMessage());
            txnDto = paymentStatusService.processStatus(txnDto);
            log.info("Updated txnDto to FAILED due to error: {}", txnDto);

            throw e; // re-throw to Global Exception handling
        }

        // Update DB as PENDING, providerReference
        txnDto.setTxnStatus(TransactionStatusEnum.PENDING.name());
        txnDto.setProviderReference(successResponse.getId());

        txnDto = paymentStatusService.processStatus(txnDto);
        log.info("Updated txnDto to PENDING: {}", txnDto);

        // return the url back to the invoker.
        TxnResponse txnResponse = new TxnResponse();
        txnResponse.setTxnReference(txnDto.getTxnReference());
        txnResponse.setTxnStatus(txnDto.getTxnStatus());
        txnResponse.setRedirectUrl(successResponse.getUrl());
        log.info("Final TxnResponse to be returned: {}", txnResponse);

        return txnResponse;
    }

}
