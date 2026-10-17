package org.ikigaidigital.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.port.TimeDepositRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimeDepositService {

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    /**
     * All time deposits with their withdrawals, ordered by id.
     */
    public List<TimeDepositDetails> getAll() {
        return repository.findAllWithWithdrawals();
    }

    /**
     * Applies one month of interest to every time deposit in a single atomic operation.
     *
     * <p>{@code days} is not advanced: no requirement says who owns that counter, and {@code TimeDeposit} has no
     * setter for it. Calling this once per accrual period is the caller's responsibility (ADR 0003).
     *
     * @throws org.ikigaidigital.domain.port.ConcurrentUpdateException if another update changed the same deposits
     *     concurrently; in that case nothing is applied
     */
    public void applyMonthlyInterest() {
        repository.updateBalancesAtomically(calculator::updateBalance);
    }
}
