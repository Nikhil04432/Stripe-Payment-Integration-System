package com.hulkhiretech.payments.service.recon;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.http.PaymentClient;
import com.hulkhiretech.payments.pojo.PaymentResponse;
import com.hulkhiretech.payments.service.interfaces.PaymentStatusService;
import com.hulkhiretech.payments.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconService {

    // -----------------------------------------------------------------------
    // DEPENDENCIES injected by Spring (via @RequiredArgsConstructor)
    // -----------------------------------------------------------------------
    private final TransactionDao transactionDao;   // DB operations
    private final PaymentClient paymentClient;     // calls Stripe's GET /payments/{sessionId}
    private final PaymentStatusService paymentStatusService; // updates txn status in DB
    private final ModelMapper modelMapper;         // converts Entity <-> DTO

    // -----------------------------------------------------------------------
    // THRESHOLD: how many times we re-check before we give up on this payment
    //
    // MAX_RETRY_COUNT = 3 (defined in Constant.java)
    //   retry 0 → 12:15 PM  → first check
    //   retry 1 → 12:30 PM  → second check
    //   retry 2 → 12:45 PM  → third check
    //   retry 3 → threshold reached → expire session on Stripe + mark FAILED
    // -----------------------------------------------------------------------

    @Transactional  // all DB operations inside this method are in ONE transaction
    public void reconcilePendingTransactions() {

        log.info("========== RECON JOB STARTED ==========");

        // STEP 1: Fetch ALL transactions whose status is PENDING from DB
        // These are payments where the customer opened Stripe's hosted page
        // but we don't know yet if they paid or not.
        List<TransactionEntity> pendingTxns =
                transactionDao.findAllByStatusId(TransactionStatusEnum.PENDING.getId());

        log.info("Found {} PENDING transactions to reconcile", pendingTxns.size());

        // STEP 2: Process each PENDING transaction one by one
        for (TransactionEntity txn : pendingTxns) {
            try {
                reconcileSingleTransaction(txn);
            } catch (Exception e) {
                // If one transaction fails, we log and continue with the rest.
                // We do NOT let one failure crash the entire recon job.
                log.error("Error reconciling txn id={} txnRef={} | error: {}",
                        txn.getId(), txn.getTxnReference(), e.getMessage(), e);
            }
        }

        log.info("========== RECON JOB COMPLETED ==========");
    }

    // -----------------------------------------------------------------------
    // Core logic for a SINGLE transaction
    // -----------------------------------------------------------------------
    private void reconcileSingleTransaction(TransactionEntity txn) {

        log.info("--- Reconciling txn id={} | txnRef={} | retryCount={}",
                txn.getId(), txn.getTxnReference(), txn.getRetryCount());

        // Safety guard: only process PENDING transactions
        // (We already filtered by PENDING in the query, but double-checking is safe)
        if (txn.getTxnStatusId() != TransactionStatusEnum.PENDING.getId()) {
            log.warn("Skipping txn id={} — status is NOT PENDING", txn.getId());
            return;
        }

        // Safety guard: providerReference (Stripe session ID) must exist
        // Without it, we cannot call Stripe's API to check payment status
        String providerReference = txn.getProviderReference();
        if (providerReference == null || providerReference.isBlank()) {
            log.warn("Skipping txn id={} — providerReference is null/empty. Cannot query Stripe.", txn.getId());
            return;
        }

        // ---------------------------------------------------------------
        // STEP A: Call Stripe's GET /payments/{sessionId} API
        // This tells us the current payment status from Stripe's side.
        //
        // PaymentResponse fields we care about:
        //   sessionStatus  → "open" | "complete" | "expired"
        //   paymentStatus  → "unpaid" | "paid" | "no_payment_required"
        // ---------------------------------------------------------------
        log.info("Calling Stripe getPayment API for providerReference: {}", providerReference);
        PaymentResponse stripePayment = paymentClient.getPaymentBySessionId(providerReference);

        log.info("Stripe response for txn id={} → sessionStatus={} | paymentStatus={}",
                txn.getId(),
                stripePayment.getSessionStatus(),
                stripePayment.getPaymentStatus());

        // ---------------------------------------------------------------
        // STEP B: Decision logic based on Stripe's response
        // ---------------------------------------------------------------

        // CASE 1: Payment is SUCCESSFUL on Stripe's side
        // Stripe returns paymentStatus = "paid"
        // Action: Mark our DB record as SUCCESS
        if ("paid".equalsIgnoreCase(stripePayment.getPaymentStatus())) {
            log.info("Payment SUCCESSFUL on Stripe for txn id={}. Updating DB to SUCCESS.", txn.getId());
            markAsSuccess(txn);
            return; // done for this txn, move to next one
        }

        // CASE 2: Payment is FAILED on Stripe's side
        // Stripe may return sessionStatus = "expired" or paymentStatus = "failed"
        // Action: Mark our DB record as FAILED
        if ("expired".equalsIgnoreCase(stripePayment.getSessionStatus())
                || "failed".equalsIgnoreCase(stripePayment.getPaymentStatus())) {
            log.info("Payment FAILED/EXPIRED on Stripe for txn id={}. Updating DB to FAILED.", txn.getId());
            markAsFailed(txn, "RECON_STRIPE_EXPIRED", "Payment session expired or failed on Stripe");
            return;
        }

        // CASE 3: Payment is still UNPAID — customer hasn't completed payment yet
        // paymentStatus = "unpaid" means the Stripe page is still open or customer is still deciding
        // We need to check the retry count to decide what to do.

        int currentRetryCount = txn.getRetryCount() == null ? 0 : txn.getRetryCount();
        log.info("Payment still UNPAID on Stripe for txn id={}. Current retryCount={}. Max={}",
                txn.getId(), currentRetryCount, Constant.MAX_RETRY_COUNT);

        // ---------------------------------------------------------------
        // CASE 3A: Retry count has reached the THRESHOLD
        //
        // We gave the customer enough time (3 cycles × 15 min = 45 minutes).
        // Now we:
        //   1. Call Stripe's expire API → forcefully close the hosted page
        //      (so customer cannot complete payment even if they come back)
        //   2. Mark our DB record as FAILED
        // ---------------------------------------------------------------
        if (currentRetryCount >= Constant.MAX_RETRY_COUNT) {
            log.info("Retry threshold reached for txn id={}. Expiring Stripe session and marking FAILED.", txn.getId());

            // Expire the session on Stripe's side
            // This ensures even if the customer tries to pay now, they cannot.
            expireStripeSession(txn, providerReference);

            // Mark the transaction as FAILED in our DB
            markAsFailed(txn, "RECON_MAX_RETRY_EXCEEDED",
                    "Payment not completed by customer within allowed time. Session expired.");
            return;
        }

        // ---------------------------------------------------------------
        // CASE 3B: Retry count is still below threshold
        // Customer might still be on the Stripe page, filling in card details etc.
        // Action: Just increment the retry count and leave status as PENDING.
        //         The next scheduler run (15 mins later) will check again.
        // ---------------------------------------------------------------
        log.info("Payment still in progress for txn id={}. Incrementing retryCount from {} to {}.",
                txn.getId(), currentRetryCount, currentRetryCount + 1);

        incrementRetryCount(txn, currentRetryCount);
    }

    // -----------------------------------------------------------------------
    // HELPER: Mark a transaction as SUCCESS in DB
    // -----------------------------------------------------------------------
    private void markAsSuccess(TransactionEntity txn) {
        // Convert Entity → DTO (because PaymentStatusService works with DTO)
        TransactionDTO txnDto = modelMapper.map(txn, TransactionDTO.class);

        // Set the new status
        txnDto.setTxnStatus(TransactionStatusEnum.SUCCESS.name());

        // processStatus() → finds the correct StatusProcessor (SuccessStatusProcessor)
        // → calls updateTransactionStatusDetailsByTxnReference in DAO → updates DB
        paymentStatusService.processStatus(txnDto);

        log.info("DB updated to SUCCESS for txn id={} | txnRef={}", txn.getId(), txn.getTxnReference());
    }

    // -----------------------------------------------------------------------
    // HELPER: Mark a transaction as FAILED in DB with error info
    // -----------------------------------------------------------------------
    private void markAsFailed(TransactionEntity txn, String errorCode, String errorMessage) {
        TransactionDTO txnDto = modelMapper.map(txn, TransactionDTO.class);

        txnDto.setTxnStatus(TransactionStatusEnum.FAILED.name());
        txnDto.setErrorCode(errorCode);
        txnDto.setErrorMessage(errorMessage);

        // processStatus() → FailedStatusProcessor → checks if already in final state
        // → updates DB with FAILED status + errorCode + errorMessage
        paymentStatusService.processStatus(txnDto);

        log.info("DB updated to FAILED for txn id={} | txnRef={} | errorCode={}",
                txn.getId(), txn.getTxnReference(), errorCode);
    }

    // -----------------------------------------------------------------------
    // HELPER: Increment retry count — keeps status as PENDING, just updates retryCount
    // -----------------------------------------------------------------------
    private void incrementRetryCount(TransactionEntity txn, int currentRetryCount) {
        // Update only the retryCount field in DB
        // Status remains PENDING — we are just recording that we checked once more
        txn.setRetryCount(currentRetryCount + 1);

        // We directly call DAO here because we don't need a full status change.
        // We only need to update the retryCount column.
        transactionDao.updateRetryCountByTxnReference(txn);

        log.info("retryCount incremented to {} for txn id={}", currentRetryCount + 1, txn.getId());
    }

    // -----------------------------------------------------------------------
    // HELPER: Call Stripe expire API to forcefully close the session
    // -----------------------------------------------------------------------
    private void expireStripeSession(TransactionEntity txn, String providerReference) {
        try {
            // PaymentClient.expirePaymentBySessionId() calls:
            // POST https://api.stripe.com/v1/checkout/sessions/{id}/expire
            // This makes the Stripe-hosted payment page inaccessible to customer
            paymentClient.expirePaymentBySessionId(providerReference);
            log.info("Stripe session expired successfully for txn id={} | providerRef={}",
                    txn.getId(), providerReference);
        } catch (Exception e) {
            // Even if expiry fails (e.g., session already expired), we still mark as FAILED.
            // The important thing is our DB is consistent.
            log.warn("Failed to expire Stripe session for txn id={}. Proceeding to mark FAILED anyway. Error: {}",
                    txn.getId(), e.getMessage());
        }
    }
}