package com.hulkhiretech.payments.service.helper;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.StripeProviderException;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.pojo.CreatePaymentRequest;
import com.hulkhiretech.payments.pojo.LineItem;
import com.hulkhiretech.payments.pojo.PaymentResponse;
import com.hulkhiretech.payments.stripe.StripeResponse;
import com.hulkhiretech.payments.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreatePaymentHelper {
    @Value("${stripe.create.session.url}")
    private String createSessionUrl ;

    @Value("${stripe.api.key}")
    private String stripeApiKey;
    private final ChatClient chatClient;
    private final JsonUtil jsonUtil;

    public HttpRequest prepareHttpRequest(CreatePaymentRequest createPaymentRequest) {
        log.info("preparing httpRequest in helper || createPaymentRequest: {}", createPaymentRequest);
        HttpHeaders headers = new HttpHeaders();

        headers.setBasicAuth(stripeApiKey, Constant.EMPTY_STRING);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // prepare form data
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add(Constant.MODE, Constant.MODE_PAYMENT);
        requestBody.add(Constant.SUCCESS_URL, createPaymentRequest.getSuccessUrl());
        requestBody.add(Constant.CANCEL_URL, createPaymentRequest.getCancelUrl());

        for(int i = 0; i < createPaymentRequest.getLineItems().size(); i++){
            LineItem lineItem = createPaymentRequest.getLineItems().get(i);

            requestBody.add(String.format("line_items[%d][price_data][currency]", i), lineItem.getCurrency());
            requestBody.add(String.format("line_items[%d][price_data][product_data][name]", i), lineItem.getProductName());
            requestBody.add(String.format("line_items[%d][price_data][unit_amount]", i), String.valueOf((int)(lineItem.getUnitAmount() * 100)));
            requestBody.add(String.format("line_items[%d][quantity]", i), String.valueOf(lineItem.getQuantity()));
        }


        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setHttpMethod(HttpMethod.POST);

        httpRequest.setUrl(createSessionUrl);
        httpRequest.setRequestBody(requestBody);
        httpRequest.setHeaders(headers);

        log.info("Prepared httpRequest in helper || httpRequest: {}", httpRequest);
        return httpRequest;
    }

    public StripeResponse processResponse(ResponseEntity<String> httpResponse) {
        log.info("Processing response: {}", httpResponse);

        if (httpResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Successful response received from Stripe");

            StripeResponse response = jsonUtil.convertJsonToObject(httpResponse.getBody(), StripeResponse.class);
            log.info("Converted PaymentResponse object: {}", response);

            if (response != null && response.getId() != null && response.getUrl() != null) {
                log.info("Valid PaymentResponse object created ");
                return response;
            }
        }

        // If we reach here, it means the response was not successful or invalid
        String errorResponse = prepareErrorMessageSummary(httpResponse);
        // if it is 4xx or 5xx then throw payment failed exception

        if(httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError()) {
            log.error("Error response received from Stripe: {}", httpResponse);
            throw new StripeProviderException(
                    ErrorCodeEnum.STRIPE_ERROR.getErrorCode(),
                    errorResponse,             // dynamic string message based on stripe error
                    HttpStatus.valueOf(httpResponse.getStatusCode().value())
            );
        }

        // write return statement to throw generic exception
        throw new StripeProviderException(
                ErrorCodeEnum.PAYMENT_CREATION_FAILED.getErrorCode(),
                ErrorCodeEnum.PAYMENT_CREATION_FAILED.getErrorMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );

    }

    private String prepareErrorMessageSummary(ResponseEntity<String> httpResponse) {
        // TODO : currently we are returning the entire json error response from stripe, to work without AI model
        if(true){
            return httpResponse.getBody();
        }
        String promptTemplate = """
				Given the following json message from a third-party API, read the entire JSON, and summarize in 1 line:
				Instructions:
				1. Put a short, simple summary. Which exactly represents what error happened.
				2. Max length of summary less than 200 characters.
				3. Keep the output clear and concise.
				4. Summarize as message that we can send in API response to the client.
				5. Dont point any info to read external documentation or link.
				{error_json}
				""";

        String errorJson = httpResponse.getBody();

        String response = chatClient.prompt()
                .system("You are an technical analyst. which just retunrs 1 line summary of the json error")
                .user(promptUserSpec -> promptUserSpec
                        .text(promptTemplate)
                        .param("error_json", errorJson))
                .call()
                .content();

        log.info("AI model response : {}", response);
        return response;
    }
}
