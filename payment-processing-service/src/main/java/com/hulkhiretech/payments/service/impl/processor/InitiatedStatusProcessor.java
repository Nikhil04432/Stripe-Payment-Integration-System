package com.hulkhiretech.payments.service.impl.processor;

import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.service.interfaces.TxnStatusProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InitiatedStatusProcessor implements TxnStatusProcessor {
    private final TransactionDao transactionDao;
    private final ModelMapper modelMapper;

    @Override
    public TransactionDTO processStatus(TransactionDTO txnDto) {
        log.info("Processing INITIATE Status");
        //TODO
        TransactionEntity txnEntity = modelMapper.map(txnDto, TransactionEntity.class);
        log.info("Mapped TransactionEntity: {}", txnEntity);

        transactionDao.updateTransactionStatusDetailsByTxnReference(txnEntity);
        log.info("Updated transaction status in DB for txnReference: {}", txnDto.getTxnReference());

        return txnDto;
    }
}
