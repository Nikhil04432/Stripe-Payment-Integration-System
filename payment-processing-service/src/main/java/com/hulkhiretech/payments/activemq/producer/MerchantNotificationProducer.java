package com.hulkhiretech.payments.activemq.producer;

import com.hulkhiretech.payments.constant.QueueConstant;
import com.hulkhiretech.payments.activemq.payload.PaymentNotificationMessage;
import com.hulkhiretech.payments.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MerchantNotificationProducer {

    // JmsTemplate — the Spring tool to send messages to ActiveMQ queue
    private final JmsTemplate jmsTemplate;

    // JsonUtil — to convert our PaymentNotificationMessage object to JSON string
    // WHY JSON? Because it's readable, easy to debug, and works across different
    // tech stacks (merchant might not be Java — they can still parse JSON)
    private final JsonUtil jsonUtil;

    // -----------------------------------------------------------------------
    // This method is called by SuccessStatusProcessor and FailedStatusProcessor
    // after they update the DB to SUCCESS or FAILED.
    //
    // Flow:
    //   1. Convert PaymentNotificationMessage → JSON string
    //   2. Send JSON string to ActiveMQ queue
    //   3. ActiveMQ holds the message until merchant consumer picks it up
    //
    // WHY async via MQ instead of direct HTTP call to merchant?
    //   - Merchant system might be down. MQ holds the message safely.
    //   - Our system doesn't have to wait for merchant's response.
    //   - Decoupled: merchant can process at their own pace.
    // -----------------------------------------------------------------------
    public void sendPaymentNotification(PaymentNotificationMessage message) {
        log.info("Publishing payment notification to MQ || txnReference: {} | status: {}",
                message.getTxnReference(), message.getTxnStatus());

        // Convert object to JSON string
        String messageAsJson = jsonUtil.convertObjectToJson(message);
        log.info("Message as JSON: {}", messageAsJson);

        if (messageAsJson == null) {
            // If JSON conversion fails, log and return — don't crash the status update flow
            log.error("Failed to convert PaymentNotificationMessage to JSON. Skipping MQ publish.");
            return;
        }

        // jmsTemplate.convertAndSend(queueName, message)
        // → internally opens JMS session → creates TextMessage → sends to queue → closes session
        jmsTemplate.convertAndSend(QueueConstant.PAYMENT_NOTIFICATION_QUEUE, messageAsJson);

        log.info("Payment notification successfully published to queue: {}",
                QueueConstant.PAYMENT_NOTIFICATION_QUEUE);
    }
}