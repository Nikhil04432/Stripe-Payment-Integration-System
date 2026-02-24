package com.hulkhiretech.payments.service.recon;


import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.http.PaymentClient;
import com.hulkhiretech.payments.pojo.InitiateTxnRequest;
import com.hulkhiretech.payments.pojo.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconService {

    private final TransactionDao transactionDao;
    private final PaymentClient paymentClient;

    @Transactional
    public void reconcilePendingTransactions() {

        // Use the enum constant directly to avoid potential NPE from fromName(...)

        List<TransactionEntity> pendingTxns =
                transactionDao.findAllByStatusId(TransactionStatusEnum.PENDING.getId());

        log.info("Found {} pending transactions for reconciliation", pendingTxns.size());

        for (TransactionEntity txn : pendingTxns) {
            reconcileSingleTransaction(txn);
        }
    }

    // lightweight placeholder implementation so the class compiles and scheduler can call it.
    // Replace with real reconciliation logic (call provider, update DB, retries, notifications) as needed.
    private void reconcileSingleTransaction(TransactionEntity txn) {
        if (txn == null) {
            log.warn("Skipping null transaction in reconciliation");
            return;
        }

        log.info("Reconciling transaction id={} txnReference={} statusId={}", txn.getId(), txn.getTxnReference(), txn.getTxnStatusId());

        // TODO: implement reconciliation with provider and update transaction via transactionDao

        // Safety guard
        if (TransactionStatusEnum.PENDING.getId() != txn.getTxnStatusId()) {
            log.warn("Transaction id={} is not pending, skipping reconciliation", txn.getId());
            return;
        }

        // call stripe's getSession and update transaction status based on response (success, failed, still pending)

        String providerReference = txn.getProviderReference();
        if (providerReference == null || providerReference.isEmpty()) {
            log.warn("Transaction id={} has no provider reference, cannot reconcile", txn.getId());
            return;
        }
        PaymentResponse paymentBySessionId = paymentClient.getPaymentBySessionId(providerReference);

        log.info("Object is successfully retrieve from Stripe Provider {} : ", paymentBySessionId);

        // add logic for retry count and max retry limit to avoid infinite retries in case of persistent failures

         // Based on paymentBySessionId status, update transaction status in DB
    }


}