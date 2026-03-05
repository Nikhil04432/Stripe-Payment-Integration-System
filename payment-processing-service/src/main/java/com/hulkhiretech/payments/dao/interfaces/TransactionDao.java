package com.hulkhiretech.payments.dao.interfaces;

import com.hulkhiretech.payments.entity.TransactionEntity;

import java.util.List;
import java.util.Optional;

public interface TransactionDao {
	
	public Integer insertTransaction(TransactionEntity txn);
	public TransactionEntity getTransactionByTxnReference(String txnRef);
	public Integer updateTransactionStatusDetailsByTxnReference(TransactionEntity txnEntity);

	public TransactionEntity getTransactionByProviderReference(String providerReference, int providerId);

    public List<TransactionEntity> findAllByStatusId(Integer txnStatusId);

    // -----------------------------------------------------------------------
    // NEW METHOD: updates ONLY the retryCount column for a given txnReference
    // Used by ReconService when payment is still UNPAID — we don't change the
    // status (stays PENDING), we just record that we checked one more time.
    // -----------------------------------------------------------------------
    public Integer updateRetryCountByTxnReference(TransactionEntity txnEntity);

    // -----------------------------------------------------------------------
    // IDEMPOTENCY CHECK METHOD
    //
    // WHY Optional<TransactionEntity> instead of TransactionEntity?
    //
    // jdbcTemplate.queryForObject() throws EmptyResultDataAccessException
    // if no row is found. We don't want an exception just because no duplicate
    // exists — that is a normal case (first-time request).
    //
    // Optional<> cleanly represents "record exists" vs "record does not exist"
    // without throwing exceptions. Caller checks:
    //   optional.isPresent() → duplicate found → return existing response
    //   optional.isEmpty()   → fresh request   → proceed normally
    // -----------------------------------------------------------------------
    Optional<TransactionEntity> findByMerchantTransactionReference(
            String merchantTransactionReference);



}
