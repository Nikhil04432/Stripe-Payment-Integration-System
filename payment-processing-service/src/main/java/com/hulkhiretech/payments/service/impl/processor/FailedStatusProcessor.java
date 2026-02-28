package com.hulkhiretech.payments.service.impl.processor;

import com.hulkhiretech.payments.activemq.payload.PaymentNotificationMessage;
import com.hulkhiretech.payments.activemq.producer.MerchantNotificationProducer;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.service.helper.PaymentProcessorHelper;
import com.hulkhiretech.payments.service.interfaces.TxnStatusProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailedStatusProcessor implements TxnStatusProcessor {
	
	private final TransactionDao transactionDao;
	
	private final ModelMapper modelMapper;
	
	private final PaymentProcessorHelper paymentProcessorHelper;

    private final MerchantNotificationProducer merchantNotificationProducer;

	@Override
	public TransactionDTO processStatus(TransactionDTO txnDto) {
		log.info("Processing FAILED status for txnDto: {}", txnDto);
		
		if(paymentProcessorHelper.isTxnInFinalState(txnDto)) {
			log.warn("Transaction is already in a final state. No update performed for txnReference: {}",
					txnDto.getTxnReference());
			return txnDto;
		}
		
		TransactionEntity txnEntity = modelMapper
				.map(txnDto, TransactionEntity.class);
		log.info("Mapped txnEntity: {}", txnEntity);
		
		transactionDao.updateTransactionStatusDetailsByTxnReference(
				txnEntity);
		
		log.info("Updated transaction status successfully for txnReference: {}", 
				txnDto.getTxnReference());

        publishMerchantNotification(txnDto);
		
		return txnDto;
	}

    private void publishMerchantNotification(TransactionDTO txnDto) {

        try{
            PaymentNotificationMessage message = PaymentNotificationMessage.builder()
                    .txnReference(txnDto.getTxnReference())
                    .merchantTransactionReference(txnDto.getMerchantTransactionReference())
                    .txnStatus("FAILED")
                    .amount(txnDto.getAmount())
                    .currency(txnDto.getCurrency())
                    .build();

            // This will send the message to ActiveMQ queue, and merchant system will consume it to update their order status.
            merchantNotificationProducer.sendPaymentNotification(message);

        }catch (Exception e){
            log.error("Failed to publish payment notification to MQ for txnReference: {}. Error: {}",
                    txnDto.getTxnReference(), e.getMessage(), e);
        }
    }
	
}
