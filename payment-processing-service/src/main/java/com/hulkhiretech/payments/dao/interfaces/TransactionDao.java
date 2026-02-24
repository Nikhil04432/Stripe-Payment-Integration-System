package com.hulkhiretech.payments.dao.interfaces;

import com.hulkhiretech.payments.entity.TransactionEntity;

import java.util.List;

public interface TransactionDao {
	
	public Integer insertTransaction(TransactionEntity txn);
	public TransactionEntity getTransactionByTxnReference(String txnRef);
	public Integer updateTransactionStatusDetailsByTxnReference(TransactionEntity txnEntity);

	public TransactionEntity getTransactionByProviderReference(String providerReference, int providerId);

    public List<TransactionEntity> findAllByStatusId(Integer txnStatusId);



}
