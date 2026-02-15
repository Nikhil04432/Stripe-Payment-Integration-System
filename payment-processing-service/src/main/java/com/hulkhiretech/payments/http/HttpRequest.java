package com.hulkhiretech.payments.http;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Data
@Builder
public class HttpRequest {
    private HttpMethod httpMethod;
    private String uri;
    private HttpHeaders httpHeaders;
    private Object requestBody;

}
