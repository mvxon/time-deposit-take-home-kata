package org.ikigaidigital.domain.port;

import java.util.List;
import java.util.function.Consumer;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.TimeDepositDetails;

/**
 * Outbound port to the storage of time deposits.
 */
public interface TimeDepositRepository {

    /**
     * All time deposits ordered by id, each with its withdrawals ordered by id.
     */
    List<TimeDepositDetails> findAllWithWithdrawals();

    /**
     * Passes all time deposits to {@code balanceUpdate} page by page, in ascending id order, and stores the changed
     * balances. All pages are processed in one transaction, so either every balance is stored or none is.
     * Other fields are never written.
     *
     * @throws ConcurrentUpdateException if another update changed the same deposits concurrently; in that case
     *     nothing is stored
     */
    void updateBalancesAtomically(Consumer<List<TimeDeposit>> balanceUpdate);
}
