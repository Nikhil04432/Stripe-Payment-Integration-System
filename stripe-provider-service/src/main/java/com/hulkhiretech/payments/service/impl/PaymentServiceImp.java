package com.hulkhiretech.payments.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.StripeProviderException;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.http.HttpServiceEngine;
import com.hulkhiretech.payments.interfaces.PaymentService;
import com.hulkhiretech.payments.pojo.CreatePaymentRequest;
import com.hulkhiretech.payments.pojo.LineItem;
import com.hulkhiretech.payments.pojo.PaymentResponse;
import com.hulkhiretech.payments.service.helper.CreatePaymentHelper;
import com.hulkhiretech.payments.service.helper.ExpirePaymentHelper;
import com.hulkhiretech.payments.service.helper.GetPaymentHelper;
import com.hulkhiretech.payments.stripe.StripeResponse;
import com.hulkhiretech.payments.util.JsonUtil;
import com.hulkhiretech.payments.util.StripeResponseUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {

    private final HttpServiceEngine httpServiceEngine;
    private final ObjectMapper objectMapper;
    private final CreatePaymentHelper createPaymentHelper;
    private final GetPaymentHelper getPaymentHelper;
    private final ExpirePaymentHelper expirePaymentHelper;

    public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        log.info("Processing payment creation|| "
                + "createPaymentRequest: {}", createPaymentRequest);

        // if createPaymementRequest 1st line item quantity is 0 or less then throw exception
        if (createPaymentRequest.getLineItems().get(0).getQuantity() <= 0) {
            throw new StripeProviderException(
                    ErrorCodeEnum.INVALID_QUANTITY.getErrorCode(),
                    ErrorCodeEnum.INVALID_QUANTITY.getErrorMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
        HttpRequest httpRequest = createPaymentHelper.prepareHttpRequest(createPaymentRequest);

        // log the httpRequest object
        log.info("HttpRequest prepared from helper: {}", httpRequest);

        ResponseEntity<String> httpResponse = httpServiceEngine.makeHttpCall(httpRequest);
        log.info("Response from httpCall method : {}", httpResponse);

        StripeResponse stripeResponse = createPaymentHelper.processResponse(httpResponse);
        log.info("Final response from createPayment method: {}", stripeResponse);

        // convert stripeResponse to PaymentResponse
        PaymentResponse response = StripeResponseUtil.preparePaymentRespose(stripeResponse);

        return response;
    }

    public PaymentResponse getPayment(String id) {
        log.info("Processing get payment || id: {}", id);

        HttpRequest httpRequest = getPaymentHelper.prepareHttpRequest(id);

        // log the httpRequest object
        log.info("HttpRequest prepared from helper: {}", httpRequest);

        ResponseEntity<String> httpResponse = httpServiceEngine.makeHttpCall(httpRequest);
        log.info("Response from httpCall method : {}", httpResponse);

        StripeResponse stripeResponse= getPaymentHelper.processResponse(httpResponse);
        log.info("Final response from createPayment method: {}", stripeResponse);

        // convert stripeResponse to PaymentResponse
        PaymentResponse response = StripeResponseUtil.preparePaymentRespose(stripeResponse);

        return response;
    }


    public PaymentResponse expirePayment(String id) {
        log.info("Processing expire payment || id: {}", id);

        HttpRequest httpRequest = expirePaymentHelper.prepareHttpRequest(id);

        // log the httpRequest object
        log.info("HttpRequest prepared from helper: {}", httpRequest);

        ResponseEntity<String> httpResponse = httpServiceEngine.makeHttpCall(httpRequest);
        log.info("Response from httpCall method : {}", httpResponse);

        StripeResponse stripeResponse= expirePaymentHelper.processResponse(httpResponse);
        log.info("Final response from createPayment method: {}", stripeResponse);

        // convert stripeResponse to PaymentResponse
        PaymentResponse response = StripeResponseUtil.preparePaymentRespose(stripeResponse);

        return response;
    }
}
