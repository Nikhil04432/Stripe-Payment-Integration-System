package com.hulkhiretech.payments.dao.impl;

import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.entity.TransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class TransactionDaoImpl implements TransactionDao {
	
	private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
        INSERT INTO payments.`Transaction` (
            userId, paymentMethodId, providerId, paymentTypeId, txnStatusId,
            amount, currency, merchantTransactionReference, txnReference
        ) VALUES (
            :userId, :paymentMethodId, :providerId, :paymentTypeId, :txnStatusId,
            :amount, :currency, :merchantTransactionReference, :txnReference
        )
        """;

	@Override
	public Integer insertTransaction(TransactionEntity txn) {
		log.info("Inserting transaction: {}", txn);
		KeyHolder keyHolder = new GeneratedKeyHolder();
        
		BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(txn);

        jdbcTemplate.update(INSERT_SQL, params, keyHolder, new String[]{"id"});
        // set the generated id back to the entity
        txn.setId(keyHolder.getKey().intValue());
        
        log.info("Inserted transaction with generated id: {}", txn.getId());
        return txn.getId();
	}

	@Override
	public TransactionEntity getTransactionByTxnReference(String txnRef) {
		String sql = "select * from payments.`Transaction` where txnReference = :txnReference";
		log.info("Fetching transaction with txnReference: {}", txnRef);
		// Implementation pending
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("txnReference", txnRef);

		TransactionEntity txnEntity = jdbcTemplate.queryForObject(sql, params, new BeanPropertyRowMapper<>(TransactionEntity.class));
		log.info("Fetched transaction: {}", txnEntity);

		return txnEntity;

	}

	@Override
	public Integer updateTransactionStatusDetailsByTxnReference(TransactionEntity txnEntity) {
		String sql = " update payments.`Transaction` set txnStatusId = :txnStatusId, " + "providerReference = :providerReference " +
				"where txnReference = :txnReference ";

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("txnStatusId", txnEntity.getTxnStatusId());
		params.addValue("providerReference", txnEntity.getProviderReference());
		params.addValue("txnReference", txnEntity.getTxnReference());

		return jdbcTemplate.update(sql, params);
	}

	@Override
	public TransactionEntity getTransactionByProviderReference(
			String providerReference, int providerId) {
		log.info("Fetching transaction with providerReference: {} and providerId: {}",
				providerReference, providerId);

		String sql = "SELECT * FROM payments.`Transaction` "
				+ "WHERE providerReference = :providerReference "
				+ "AND providerId = :providerId";

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("providerReference", providerReference);
		params.addValue("providerId", providerId);

		TransactionEntity txnEntity = jdbcTemplate.queryForObject(
				sql,
				params,
				new BeanPropertyRowMapper<>(TransactionEntity.class));

		log.info("Fetched transaction entity: {}", txnEntity);
		return txnEntity;
	}

}
