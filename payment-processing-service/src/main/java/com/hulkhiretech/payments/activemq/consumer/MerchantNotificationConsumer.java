package com.hulkhiretech.payments.activemq.consumer;

import com.hulkhiretech.payments.constant.QueueConstant;
import com.hulkhiretech.payments.activemq.payload.PaymentNotificationMessage;
import com.hulkhiretech.payments.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

// -----------------------------------------------------------------------
// MERCHANT SIDE CONSUMER — only for cross-checking / local verification.
//
// In real world: this code lives in the MERCHANT's system, not ours.
// We've added it here just to verify our producer is working correctly.
//
// HOW @JmsListener WORKS:
//   Spring automatically starts a background listener thread.
//   Whenever a message arrives in "payment.notification.queue",
//   Spring calls onPaymentNotification() with that message string.
//   No polling, no manual loop — Spring handles it all.
// -----------------------------------------------------------------------
@Component
@Slf4j
@RequiredArgsConstructor
public class MerchantNotificationConsumer {

    private final JsonUtil jsonUtil;

    // -----------------------------------------------------------------------
    // @JmsListener:
    //   destination  → which queue to listen to
    //   containerFactory → which factory bean to use (defined in ActiveMQConfig)
    //
    // The parameter "String messageJson" receives the raw JSON string
    // that our Producer sent.
    // -----------------------------------------------------------------------
    @JmsListener(
            destination = QueueConstant.PAYMENT_NOTIFICATION_QUEUE,
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onPaymentNotification(String messageJson) {
        log.info("===== [MERCHANT CONSUMER] Message received from queue =====");
        log.info("Raw JSON received: {}", messageJson);

        // STEP 1: Convert JSON string back to Java object
        PaymentNotificationMessage message = jsonUtil.convertJsonToObject(
                messageJson, PaymentNotificationMessage.class);

        if (message == null) {
            log.error("[MERCHANT CONSUMER] Failed to parse message JSON. Skipping.");
            return;
        }

        log.info("[MERCHANT CONSUMER] Parsed PaymentNotificationMessage: {}", message);

        // STEP 2: Handle based on txnStatus
        // In real merchant system: update their own DB, send email to customer, etc.
        switch (message.getTxnStatus()) {
            case "SUCCESS":
                handleSuccess(message);
                break;
            case "FAILED":
                handleFailed(message);
                break;
            default:
                log.warn("[MERCHANT CONSUMER] Unknown txnStatus received: {}", message.getTxnStatus());
        }
    }

    // -----------------------------------------------------------------------
    // What a real merchant would do on SUCCESS:
    //   - Update order status in their DB
    //   - Send "payment received" email to customer
    //   - Trigger order fulfillment
    // Here we just log it for verification.
    // -----------------------------------------------------------------------
    private void handleSuccess(PaymentNotificationMessage message) {
        log.info("[MERCHANT CONSUMER] ✅ Payment SUCCESS received");
        log.info("[MERCHANT CONSUMER] txnRef={} | merchantRef={} | amount={} {}",
                message.getTxnReference(),
                message.getMerchantTransactionReference(),
                message.getAmount(),
                message.getCurrency());
        // TODO (merchant side): update order, send email, trigger fulfillment
    }

    // -----------------------------------------------------------------------
    // What a real merchant would do on FAILED:
    //   - Update order status as failed in their DB
    //   - Send "payment failed" email to customer
    //   - Release any reserved inventory
    // -----------------------------------------------------------------------
    private void handleFailed(PaymentNotificationMessage message) {
        log.info("[MERCHANT CONSUMER] ❌ Payment FAILED received");
        log.info("[MERCHANT CONSUMER] txnRef={} | merchantRef={} | errorCode={} | errorMessage={}",
                message.getTxnReference(),
                message.getMerchantTransactionReference(),
                message.getErrorCode(),
                message.getErrorMessage());
        // TODO (merchant side): update order as failed, notify customer
    }
}