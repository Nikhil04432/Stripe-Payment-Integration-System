package com.hulkhiretech.payments.service.helper;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.StripeProviderException;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.stripe.StripeResponse;
import com.hulkhiretech.payments.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpirePaymentHelper {
    @Value("${stripe.expire.session.url}")
    private String expireSessionUrlTemplate;

    @Value("${stripe.api.key}")
    private String stripeApiKey;
    private final ChatClient chatClient;
    private final JsonUtil jsonUtil;

    public HttpRequest prepareHttpRequest(String id) {
        log.info("Preparing HTTP request to expire payment session for id: {}", id);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(stripeApiKey, Constant.EMPTY_STRING);

        String expireSessionUrl = expireSessionUrlTemplate.replace("{id}", id);
        log.info("Constructed expireSessionUrl: {}", expireSessionUrl);
        log.info("ExpireUrlTemplate: {}", expireSessionUrlTemplate);

        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setHttpMethod(HttpMethod.POST);
        httpRequest.setUrl(expireSessionUrl);
        httpRequest.setHeaders(headers);
        httpRequest.setRequestBody("");         // GET request typically has no body
        log.info("Prepared HttpRequest: {}", httpRequest);

        return httpRequest;
    }

    public StripeResponse processResponse(ResponseEntity<String> httpResponse) {
        log.info("Processing response: {}", httpResponse);

        if (httpResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Successful response received from Stripe");

            StripeResponse response = jsonUtil.convertJsonToObject(httpResponse.getBody(), StripeResponse.class);
            log.info("Converted PaymentResponse object: {}", response);

            if (response != null && response.getId() != null) {
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
                ErrorCodeEnum.GET_PAYMENT_FAILED.getErrorCode(),
                ErrorCodeEnum.GET_PAYMENT_FAILED.getErrorMessage(),
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
