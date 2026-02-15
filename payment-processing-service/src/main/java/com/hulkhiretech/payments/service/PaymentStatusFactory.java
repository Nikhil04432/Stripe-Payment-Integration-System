package com.hulkhiretech.payments.service;

import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.service.impl.processor.*;
import com.hulkhiretech.payments.service.interfaces.TxnStatusProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentStatusFactory {

    private final ApplicationContext context;

    public TxnStatusProcessor getStatusProcessor(TransactionStatusEnum statusEnum) {
        log.info("Fetching status processor for statusEnum: {}", statusEnum);

        switch (statusEnum) {
            case CREATED:
                return context.getBean(CreatedStatusProcessor.class);
            case INITIATED:
                return context.getBean(InitiatedStatusProcessor.class);
            case PENDING:
                return context.getBean(PendingStatusProcessor.class);
            case SUCCESS:
                return context.getBean(SuccessStatusProcessor.class);
            case FAILED:
                return context.getBean(FailedStatusProcessor.class);
            default:
                return null;
        }
    }
}
