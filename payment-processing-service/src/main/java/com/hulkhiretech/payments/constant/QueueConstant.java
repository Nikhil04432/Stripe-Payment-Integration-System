package com.hulkhiretech.payments.constant;

// -----------------------------------------------------------------------
// All ActiveMQ queue names in one place.
// WHY? If we ever rename a queue, we change it here — not scattered everywhere.
// This is the same principle as having a Constant.java for URL paths.
// -----------------------------------------------------------------------
public class QueueConstant {

    private QueueConstant() {} // prevent instantiation

    // Queue name: payment-processing-service publishes here
    // merchant-system (consumer) subscribes to this queue
    public static final String PAYMENT_NOTIFICATION_QUEUE = "payment.notification.queue";
}