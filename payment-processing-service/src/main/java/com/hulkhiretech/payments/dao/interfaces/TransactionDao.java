package com.hulkhiretech.payments.dao.interfaces;

import com.hulkhiretech.payments.entity.TransactionEntity;

import java.util.List;

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



}
