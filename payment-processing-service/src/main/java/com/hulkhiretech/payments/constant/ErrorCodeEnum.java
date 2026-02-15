package com.hulkhiretech.payments.constant;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    GENERIC_ERROR("30000", "Unable to process the request. Please try again later."),
    UNABLE_TO_CONNECT_TO_STRIPE_PROVIDER("30002", "Unable to connect to stripe"),
    UNABLE_TO_INITIATE_PAYMENT("30003", "Payment initiation failed");

    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
