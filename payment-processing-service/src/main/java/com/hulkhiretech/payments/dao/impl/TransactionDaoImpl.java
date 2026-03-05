package com.hulkhiretech.payments.dao.impl;

import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.entity.TransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    @Override
    public List<TransactionEntity> findAllByStatusId(Integer txnStatusId) {
        log.info("Fetching transactions with txnStatusId: {}", txnStatusId);

        String sql = "SELECT * FROM payments.`Transaction` "
                + "WHERE txnStatusId = :txnStatusId";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("txnStatusId", txnStatusId);

        List<TransactionEntity> transactions = jdbcTemplate.query(
                sql,
                params,
                new BeanPropertyRowMapper<>(TransactionEntity.class)
        );

        log.info("Fetched {} transactions with status: {}", transactions.size(), txnStatusId);
        return transactions;
    }

    // -----------------------------------------------------------------------
    // NEW METHOD: Updates ONLY the retryCount column.
    //
    // WHY a separate method?
    //   - updateTransactionStatusDetailsByTxnReference() updates txnStatusId
    //     + providerReference. We don't want to touch those here.
    //   - In recon, when payment is still UNPAID, the status stays PENDING.
    //     We only want to record "we checked one more time" = retryCount++
    //   - Keeping SQL narrow (updates only what's needed) is safer and clearer.
    // -----------------------------------------------------------------------
    @Override
    public Integer updateRetryCountByTxnReference(TransactionEntity txnEntity) {
        log.info("Updating retryCount for txnReference={} to retryCount={}",
                txnEntity.getTxnReference(), txnEntity.getRetryCount());

        // SQL: only update the retryCount column, nothing else
        String sql = "UPDATE payments.`Transaction` SET retryCount = :retryCount "
                + "WHERE txnReference = :txnReference";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("retryCount", txnEntity.getRetryCount());
        params.addValue("txnReference", txnEntity.getTxnReference());

        int rowsUpdated = jdbcTemplate.update(sql, params);
        log.info("retryCount updated. Rows affected: {}", rowsUpdated);
        return rowsUpdated;
    }

    // -----------------------------------------------------------------------
    // IDEMPOTENCY: findByMerchantTransactionReference
    //
    // This is called BEFORE inserting a new transaction.
    // If a record already exists with this merchantTransactionReference,
    // we return it wrapped in Optional.of(entity).
    // If no record found, we return Optional.empty() — no exception thrown.
    //
    // WHY try-catch EmptyResultDataAccessException?
    // queryForObject() throws this exception when 0 rows returned.
    // We treat "0 rows" as a valid case (fresh request) — not an error.
    // So we catch it and return Optional.empty() instead of letting it
    // propagate as an exception.
    // -----------------------------------------------------------------------
    @Override
    public Optional<TransactionEntity> findByMerchantTransactionReference(
            String merchantTransactionReference) {

        log.info("Idempotency check for merchantTransactionReference: {}",
                merchantTransactionReference);

        String sql = "SELECT * FROM payments.`Transaction` "
                + "WHERE merchantTransactionReference = :merchantTransactionReference";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("merchantTransactionReference", merchantTransactionReference);

        try {
            TransactionEntity entity = jdbcTemplate.queryForObject(
                    sql, params,
                    new BeanPropertyRowMapper<>(TransactionEntity.class));

            log.info("Duplicate found for merchantTransactionReference: {}. "
                            + "Returning existing txnReference: {}",
                    merchantTransactionReference, entity.getTxnReference());

            return Optional.of(entity);

        } catch (EmptyResultDataAccessException e) {
            // This is NOT an error — it means no duplicate exists.
            // This is the happy path for a fresh first-time request.
            log.info("No duplicate found for merchantTransactionReference: {}. "
                    + "Proceeding with new transaction.", merchantTransactionReference);
            return Optional.empty();
        }
    }

}
