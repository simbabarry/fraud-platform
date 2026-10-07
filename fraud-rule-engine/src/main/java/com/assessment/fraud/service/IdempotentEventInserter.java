package com.assessment.fraud.service;

import com.assessment.fraud.domain.TransactionEvent;
import com.assessment.fraud.repository.TransactionEventRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts a transaction event in an independent transaction.
 * Unique-key collisions must propagate out of this method so Spring can roll
 * back the short {@code REQUIRES_NEW} transaction before the caller continues.
 */
@Service
@RequiredArgsConstructor
public class IdempotentEventInserter {

    private final TransactionEventRepository transactionEventRepository;

    /**
     * @return the persisted event if this caller won the insert; empty if {@code externalId} already exists
     * @throws org.springframework.dao.DataIntegrityViolationException on a concurrent unique conflict
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<TransactionEvent> insertIfAbsent(TransactionEvent event) {
        if (transactionEventRepository.existsByExternalId(event.getExternalId())) {
            return Optional.empty();
        }
        return Optional.of(transactionEventRepository.saveAndFlush(event));
    }
}
