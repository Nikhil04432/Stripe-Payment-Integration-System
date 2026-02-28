package com.hulkhiretech.payments.activemq.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// -----------------------------------------------------------------------
// This is the MESSAGE PAYLOAD we send over ActiveMQ to the merchant.
//
// WHY a separate class?
//   - We don't want to expose our internal TransactionDTO or TransactionEntity
//     to the merchant. The merchant only needs to know:
//     txnReference, merchantTransactionReference, final status, amount, currency.
//   - This is the "contract" between our system and the merchant system.
//   - If we change our internal entity, merchant message stays stable.
//
// Implements Serializable because JMS can serialize Java objects.
// We will convert this to JSON string before sending (cleaner, human-readable).
// -----------------------------------------------------------------------
@Data
@Builder        // allows: PaymentNotificationMessage.builder().txnReference("xyz").build()
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotificationMessage implements Serializable {

    private String txnReference;                 // our internal reference
    private String merchantTransactionReference; // merchant's own reference
    private String txnStatus;                    // "SUCCESS" or "FAILED"
    private BigDecimal amount;
    private String currency;
    private String errorCode;                    // null if SUCCESS
    private String errorMessage;                 // null if SUCCESS
}