package com.hulkhiretech.payments;

import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.http.HttpServiceEngine;
import com.hulkhiretech.payments.pojo.CreateTxnRequest;
import com.hulkhiretech.payments.pojo.InitiateTxnRequest;
import com.hulkhiretech.payments.pojo.TxnResponse;
import com.hulkhiretech.payments.service.helper.SPCreatePaymentHelper;
import com.hulkhiretech.payments.service.impl.PaymentServiceImpl;
import com.hulkhiretech.payments.service.interfaces.PaymentStatusService;
import com.hulkhiretech.payments.stripeprovider.StripeProviderPaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentStatusService paymentStatusService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TransactionDao transactionDao;

    @Mock
    private HttpServiceEngine httpServiceEngine;

    @Mock
    private SPCreatePaymentHelper spCreatePaymentHelper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void testCreateTxn() {
        // Arrange
        CreateTxnRequest request = new CreateTxnRequest();
        TransactionDTO mappedDto = new TransactionDTO();
        mappedDto.setTxnStatus(TransactionStatusEnum.CREATED.name());

        when(modelMapper.map(request, TransactionDTO.class)).thenReturn(mappedDto);
        when(paymentStatusService.processStatus(mappedDto)).thenReturn(mappedDto);

        // Act
        TxnResponse response = paymentService.createTxn(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTxnReference());
        assertEquals(TransactionStatusEnum.CREATED.name(), response.getTxnStatusId());

        // Verify interactions
        verify(modelMapper).map(request, TransactionDTO.class);
        verify(paymentStatusService).processStatus(mappedDto);
    }

    @Test
    void testInitiateTxn() {
        // Arrange
        String txnReference = "abc-123";
        InitiateTxnRequest initiateRequest = new InitiateTxnRequest();

        TransactionEntity txnEntity = new TransactionEntity();
        txnEntity.setTxnReference(txnReference);

        TransactionDTO dto = new TransactionDTO();
        dto.setTxnReference(txnReference);

        HttpRequest httpRequest = HttpRequest.builder().build();

        StripeProviderPaymentResponse stripeResponse = new StripeProviderPaymentResponse();
        stripeResponse.setId("provider-123");
        stripeResponse.setUrl("https://redirect-url");

        when(transactionDao.getTransactionByTxnReference(txnReference)).thenReturn(txnEntity);
        when(modelMapper.map(txnEntity, TransactionDTO.class)).thenReturn(dto);
        when(paymentStatusService.processStatus(dto)).thenReturn(dto);
        when(spCreatePaymentHelper.prepareHttpRequest(initiateRequest)).thenReturn(httpRequest);
        when(httpServiceEngine.makeHttpCall(httpRequest)).thenReturn(ResponseEntity.ok("success"));
        when(spCreatePaymentHelper.processResponse(any())).thenReturn(stripeResponse);
        when(paymentStatusService.processStatus(dto)).thenReturn(dto);

        // Act
        TxnResponse response = paymentService.initiateTxn(txnReference, initiateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(txnReference, response.getTxnReference());
        assertEquals(stripeResponse.getUrl(), response.getRedirectUrl());

        // Verify key interactions
        verify(transactionDao).getTransactionByTxnReference(txnReference);
        verify(spCreatePaymentHelper).prepareHttpRequest(initiateRequest);
        verify(httpServiceEngine).makeHttpCall(httpRequest);
        verify(spCreatePaymentHelper).processResponse(any());
    }
}
