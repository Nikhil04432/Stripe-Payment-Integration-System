package com.hulkhiretech.payments.http;

import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.ProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Component                       // @Service for just business logic, here we just need bean of this class so @Component is enough
@Slf4j
public class HttpServiceEngine {

    /* RestClient restClient = RestClient.Builder.build();
     in this way restClient object is created by calling static method builder() of RestClient class and then calling build() method of the object returned by builder() method
    */
    RestClient restClient;

    public HttpServiceEngine(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        log.info("restClient object created in HttpServiceEngine: {}", restClient);
    }


    public ResponseEntity<String> makeHttpCall( HttpRequest httpRequest) {
        log.info(" making http call ");

        try {
            ResponseEntity<String> httpResponse = restClient.method(httpRequest.getHttpMethod())   // here httpRequest.getMethod() returns HttpMethod.POST
                    .uri(httpRequest.getUri())
                    .headers(t -> t.addAll(httpRequest.getHttpHeaders()))      // here t is HttpHeaders object)
                    .body(httpRequest.getRequestBody())
                    .retrieve()

                    // actual http call is made when we call toEntity() method before that all are just setting up the request
                    .toEntity(String.class);        // here String.class means we want the response body to be converted to String type
            // why string type because we are not creating any pojo class to map the response body

            log.info("httpResponse received: {}", httpResponse);

            return httpResponse;
        } catch (HttpClientErrorException |
                 HttpServerErrorException ex) {       // here "|" is used to catch multiple exceptions in single catch block
            // valid error response from stripe, return the response entity with the status code and body from the exception to the caller
            log.info(" HttpClientErrorException | HttpServerErrorException occurred while making http call: {}", httpRequest);

            // for 504 Gateway timeout & 503 Service unavailable, we should throw Unable to connect to Stripe exception
            if (ex.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT || ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                throw new ProcessingException(
                        ErrorCodeEnum.UNABLE_TO_CONNECT_TO_STRIPE_PROVIDER.getErrorCode(),
                        ErrorCodeEnum.UNABLE_TO_CONNECT_TO_STRIPE_PROVIDER.getErrorMessage(),
                        HttpStatus.SERVICE_UNAVAILABLE
                );
            }

            // create the responseEntity  with the status code and body from the exception and return
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());

        } catch (Exception e) {// Unable to connect
            log.error("Exception occurred while making HTTP call: {}", e.getMessage(), e);

            throw new ProcessingException(
                    ErrorCodeEnum.UNABLE_TO_CONNECT_TO_STRIPE_PROVIDER.getErrorCode(),
                    ErrorCodeEnum.UNABLE_TO_CONNECT_TO_STRIPE_PROVIDER.getErrorMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }
}
