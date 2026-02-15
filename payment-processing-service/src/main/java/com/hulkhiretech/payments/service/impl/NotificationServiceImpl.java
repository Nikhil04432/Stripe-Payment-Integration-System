package com.hulkhiretech.payments.service.impl;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.constant.NotificationType;
import com.hulkhiretech.payments.constant.ProviderEnum;
import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.pojo.NotificationRequest;
import com.hulkhiretech.payments.service.interfaces.NotificationService;
import com.hulkhiretech.payments.service.interfaces.PaymentStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final ModelMapper modelMapper;

	private final TransactionDao transactionDao;
	
	private final PaymentStatusService paymentStatusService;
	
	@Override
	public void processNotification(NotificationRequest notificationRequest) {
		log.info("Processing payment notification "
				+ "||notificationRequest:{}", notificationRequest);

		ProviderEnum provider = ProviderEnum.fromName(notificationRequest.getProvider());
		
		TransactionEntity txnEntity = transactionDao.getTransactionByProviderReference(
				notificationRequest.getProviderReference(),
				provider.getId());
		TransactionDTO txnDto = modelMapper.map(txnEntity, TransactionDTO.class);
		log.info("Mapped txnDto: {}", txnDto);

		NotificationType type = NotificationType.fromName(
				notificationRequest.getNotificationType());
		log.info("Notification type identified: {}", type);

		switch (type) {
		case PAYMENT_SUCCESS:
			log.info("Handling PAYMENT_SUCCESS notification");
			
			processPaymentSuccess(notificationRequest, txnDto);
			break;
		case PAYMENT_FAILED:
			log.info("Handling PAYMENT_FAILURE notification");
			// Add logic to handle payment failure
			processPaymentFailed(notificationRequest, txnDto);
			break;
		default:
			log.warn("Unhandled notification type: {}", type);
			break;
		}
	}

	private void processPaymentSuccess(NotificationRequest notificationRequest, TransactionDTO txnDto) {
		txnDto.setTxnStatus(TransactionStatusEnum.SUCCESS.name());
		txnDto = paymentStatusService.processStatus(txnDto);
		log.info("Transaction updated to SUCCESS: {}", txnDto);
	}

	private void processPaymentFailed(NotificationRequest notificationRequest, 
			TransactionDTO txnDto) {
		txnDto.setTxnStatus(TransactionStatusEnum.FAILED.name());
		
		txnDto.setErrorCode(
				notificationRequest.getPayload().get(Constant.ERROR_CODE));
		
		txnDto.setErrorMessage(
				notificationRequest.getPayload().get(Constant.ERROR_MESSAGE));
		
		txnDto = paymentStatusService.processStatus(txnDto);
		log.info("Transaction updated to SUCCESS: {}", txnDto);
		
	}

}
