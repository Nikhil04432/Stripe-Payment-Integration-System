package com.hulkhiretech.payments.service.impl;

import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.service.PaymentStatusFactory;
import com.hulkhiretech.payments.service.interfaces.PaymentStatusService;
import com.hulkhiretech.payments.service.interfaces.TxnStatusProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentStatusServiceImpl implements PaymentStatusService {

    private final PaymentStatusFactory statusFactory;

    @Override
    public TransactionDTO processStatus(TransactionDTO txnDto) {
        log.info("Processing transaction status for txnDto: {}", txnDto);
        //TODO
        TransactionStatusEnum statusEnum = TransactionStatusEnum.fromName(txnDto.getTxnStatus());
        TxnStatusProcessor statusProcessor = statusFactory.getStatusProcessor(statusEnum);     // TxnStatusProcessor is
        // parent ( interface ) so it can store the object of subclasses which has implemented it

        if (statusProcessor == null) {
            throw new IllegalArgumentException("Invalid txnStatusId: " + txnDto.getTxnStatus());
        }

        TransactionDTO response = statusProcessor.processStatus(txnDto);
        log.info("Processed status result: {}", response);

        return response;
    }
}
