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
public class SuccessStatusProcessor implements TxnStatusProcessor {
	
	private final TransactionDao transactionDao;
	
	private final ModelMapper modelMapper;
	
	private final PaymentProcessorHelper paymentProcessorHelper;

    private final MerchantNotificationProducer merchantNotificationProducer;


	@Override
	public TransactionDTO processStatus(TransactionDTO txnDto) {
		log.info("Processing SUCCESS status for txnDto: {}", txnDto);
		
		if(paymentProcessorHelper.isTxnInFinalState(txnDto)) {
			log.warn("Transaction is already in a final state. No update performed for txnReference: {}",
					txnDto.getTxnReference());
			// Log into DB
			return txnDto;
		}
		
		TransactionEntity txnEntity = modelMapper
				.map(txnDto, TransactionEntity.class);
		log.info("Mapped txnEntity: {}", txnEntity);
		
		transactionDao.updateTransactionStatusDetailsByTxnReference(
				txnEntity);
		
		log.info("Updated transaction status successfully for txnReference: {}", 
				txnDto.getTxnReference());

        // TODO: add a message to activeMQ topic which will be send to Merchant to update
        //  the order status in their system. This will be done in next phase.

        publishMerchantNotification(txnDto);

		
		return txnDto;
	}


    // -----------------------------------------------------------------------
    // This method builds the PaymentNotificationMessage and sends it to ActiveMQ
    // using the MerchantNotificationProducer.
    // Flow:
    //   1. Create PaymentNotificationMessage with txn details
    //   2. Call merchantNotificationProducer.sendPaymentNotification(message)
    //   3. The producer converts it to JSON and sends to MQ
    //
    private void publishMerchantNotification(TransactionDTO txnDto) {
        log.info("Preparing to publish payment notification for txnReference: {}",
                txnDto.getTxnReference());

        // Build the message payload

        try {
            PaymentNotificationMessage message = PaymentNotificationMessage.builder()
                    .txnReference(txnDto.getTxnReference())
                    .merchantTransactionReference(txnDto.getMerchantTransactionReference())
                    .txnStatus("SUCCESS")
                    .amount(txnDto.getAmount())
                    .currency(txnDto.getCurrency())
                    .build();

            // Send the message to ActiveMQ
            merchantNotificationProducer.sendPaymentNotification(message);
        } catch (Exception e) {
            // If anything goes wrong during message creation or sending, log the error.

            log.error("Failed to publish payment notification for txnReference: {}. Error: {}",
                    txnDto.getTxnReference(), e.getMessage(), e);
        }

    }

	

}
