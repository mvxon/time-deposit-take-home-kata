package org.ikigaidigital.domain.model;

import java.util.List;
import org.ikigaidigital.TimeDeposit;

// TimeDeposit is a shared class that must not change, so withdrawals are attached through this read model instead.
public record TimeDepositDetails(TimeDeposit deposit, List<Withdrawal> withdrawals) {

    public TimeDepositDetails {
        withdrawals = List.copyOf(withdrawals);
    }
}
