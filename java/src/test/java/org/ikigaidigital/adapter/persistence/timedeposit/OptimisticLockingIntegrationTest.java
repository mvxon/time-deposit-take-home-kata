package org.ikigaidigital.adapter.persistence.timedeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.TimeDepositService;
import org.ikigaidigital.domain.port.ConcurrentUpdateException;
import org.ikigaidigital.domain.port.TimeDepositRepository;
import org.ikigaidigital.support.IntegrationTest;
import org.ikigaidigital.support.TimeDepositFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class OptimisticLockingIntegrationTest {

    @Autowired
    private TimeDepositRepository repository;

    @Autowired
    private TimeDepositService service;

    @Autowired
    private TimeDepositCalculator calculator;

    @Autowired
    private TimeDepositFixtures fixtures;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        fixtures.clear();
        fixtures.deposit(1, "student", 100, "10000.00");
    }

    @Test
    void rejectsUpdateWhenAnotherUpdateCommittedInBetween() {
        var concurrent = new TransactionTemplate(transactionManager);
        concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> repository.updateBalancesAtomically(deposits -> {
                    concurrent.executeWithoutResult(status -> service.applyMonthlyInterest());
                    calculator.updateBalance(deposits);
                }))
                .isInstanceOf(ConcurrentUpdateException.class);

        assertThat(fixtures.balanceOf(1)).isEqualByComparingTo(new BigDecimal("10025.00"));
    }

    @Test
    void rollsBackEarlierPagesWhenALaterPageConflicts() {
        fixtures.deposit(2, "student", 100, "10000.00");
        fixtures.deposit(3, "student", 100, "10000.00");
        var concurrent = new TransactionTemplate(transactionManager);
        concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> repository.updateBalancesAtomically(deposits -> {
                    if (deposits.getFirst().getId() == 3) {
                        concurrent.executeWithoutResult(status -> fixtures.changeConcurrently(3));
                    }
                    calculator.updateBalance(deposits);
                }))
                .isInstanceOf(ConcurrentUpdateException.class);

        assertThat(fixtures.balanceOf(1)).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(fixtures.balanceOf(2)).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(fixtures.balanceOf(3)).isEqualByComparingTo(new BigDecimal("10001.00"));
    }

    @Test
    void keepsRowsWithoutInterestUntouched() {
        fixtures.deposit(2, "basic", 10, "500.00");

        service.applyMonthlyInterest();

        assertThat(fixtures.versionOf(1)).isEqualTo(1);
        assertThat(fixtures.versionOf(2)).isZero();
    }
}
