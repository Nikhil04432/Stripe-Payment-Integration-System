package com.hulkhiretech.payments.constant;

public class Constant {
    private Constant() {
    }

    public static final String V1_PAYMENTS = "/v1/payments";

    public static final String ERROR_MESSAGE = "errorMessage";

    public static final String ERROR_CODE = "errorCode";

    public static final int MAX_RETRY_COUNT = 3;
    // Queue name: payment-processing-service publishes here
    // merchant-system (consumer) subscribes to this queue
    public static final String PAYMENT_NOTIFICATION_QUEUE = "payment.notification.queue";


}
