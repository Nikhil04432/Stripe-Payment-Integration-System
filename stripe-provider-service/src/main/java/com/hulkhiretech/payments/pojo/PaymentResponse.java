package com.hulkhiretech.payments.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
public class PaymentResponse {
    private String id;
    private String url;
    private String sessionStatus;
    private String paymentStatus;
}
