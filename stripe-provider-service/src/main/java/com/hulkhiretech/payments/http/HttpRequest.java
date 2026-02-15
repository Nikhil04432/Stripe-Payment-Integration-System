package com.hulkhiretech.payments.http;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Data
public class HttpRequest {
    private HttpMethod httpMethod;
    private String url;
    private HttpHeaders headers;
    private Object requestBody;
}
