package com.hulkhiretech.payments.service.helper;

import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.ProcessingException;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.pojo.InitiateTxnRequest;
import com.hulkhiretech.payments.stripeprovider.CreatePaymentRequest;
import com.hulkhiretech.payments.stripeprovider.StripeErrorResponse;
import com.hulkhiretech.payments.stripeprovider.StripeProviderPaymentResponse;
import com.hulkhiretech.payments.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

/**
 * This class prepares the HttpRequest object 
 * for calling Stripe Provider CreatePayment API call
 * 
 * Also processes the response
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SPCreatePaymentHelper {

	@Value("${stripe-provider.create.payment.url}")
	private String createPaymentUrl;

	private final ModelMapper modelMapper;

	private final JsonUtil jsonUtil;

	public HttpRequest prepareHttpRequest(InitiateTxnRequest initiateTxnRequest) {
		log.info("Preparing HttpRequest for InitiateTxnRequest: {}", 
				initiateTxnRequest);

		HttpHeaders httpHeaders	 = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		CreatePaymentRequest reqObj = modelMapper.map(
				initiateTxnRequest, CreatePaymentRequest.class);

		String reqAsJson = jsonUtil.convertObjectToJson(reqObj);
		log.info("Converted CreatePaymentRequest to JSON: {}", reqAsJson);

		if (reqAsJson == null) {
			throw new ProcessingException(
					ErrorCodeEnum.UNABLE_TO_INITIATE_PAYMENT.getErrorCode(),
					ErrorCodeEnum.UNABLE_TO_INITIATE_PAYMENT.getErrorMessage(), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		HttpRequest httpRequest = HttpRequest.builder()
				.httpMethod(HttpMethod.POST)
				.uri(createPaymentUrl)
				.httpHeaders(httpHeaders)
				.requestBody(reqAsJson)
				.build();

		log.info("Prepared HttpRequest: {}", httpRequest);
		return httpRequest;
	}

	public StripeProviderPaymentResponse processResponse(
			ResponseEntity<String> response) {
		log.info("Processing response from Stripe Provider CreatePayment: {}", response);

		if(response.getStatusCode().is2xxSuccessful()) {// success
			// convert json into success java object
			// check if id & url are not null
			// return success response back

			//StripeProviderPaymentResponse
			StripeProviderPaymentResponse responseObj = jsonUtil.convertJsonToObject(
					response.getBody(),
					StripeProviderPaymentResponse.class);
			log.info("Converted StripeProviderPaymentResponse: {}", responseObj);

			if (responseObj != null 
					&& responseObj.getId() != null 
					&& responseObj.getUrl() != null) {
				log.info("StripeProviderPaymentResponse is valid");
				return responseObj;
			} 

		}

		// if we reach this code, means it error & we should throw an exception.

		// if 4xx or 5xx, then also throw Payment creation failed exception
		if (response.getStatusCode().is4xxClientError() 
				|| response.getStatusCode().is5xxServerError()) {
			log.error("HTTP response indicates client or server error: {}", 
					response.getStatusCode());

			// errorCode & errorMessage
			StripeErrorResponse errorResponse = jsonUtil.convertJsonToObject(
					response.getBody(),
					StripeErrorResponse.class);
			log.info("Converted StripeErrorResponse: {}", errorResponse);
			
			
			if (errorResponse != null) {
				log.info("StripeProviderPaymentResponse is valid");
				throw new ProcessingException(
						errorResponse.getErrorCode(),
						errorResponse.getErrorMessage(),
						HttpStatus.valueOf(response.getStatusCode().value()) 
						);
			}
			

		} 

		log.error("Unexpected HTTP response status: {}", response.getStatusCode());
		
		throw new ProcessingException(
				ErrorCodeEnum.UNABLE_TO_INITIATE_PAYMENT.getErrorCode(),
				ErrorCodeEnum.UNABLE_TO_INITIATE_PAYMENT.getErrorMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR
				);
	}

}
