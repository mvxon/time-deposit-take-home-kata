package org.ikigaidigital.adapter.persistence.timedeposit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.MoneyRounding;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.port.ConcurrentUpdateException;
import org.ikigaidigital.domain.port.TimeDepositRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

    private static final int BEFORE_FIRST_ID = Integer.MIN_VALUE;

    private final TimeDepositJpaRepository repo;
    private final TimeDepositPersistenceMapper mapper;
    private final EntityManager entityManager;
    private final Limit pageSize;

    TimeDepositPersistenceAdapter(
            TimeDepositJpaRepository repo,
            TimeDepositPersistenceMapper mapper,
            EntityManager entityManager,
            @Value("${time-deposit.balance-update.page-size:1000}") int pageSize) {
        this.repo = repo;
        this.mapper = mapper;
        this.entityManager = entityManager;
        this.pageSize = Limit.of(pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        return repo.findAllWithWithdrawals().stream().map(mapper::toDetails).toList();
    }

    @Override
    @Transactional
    public void updateBalancesAtomically(Consumer<List<TimeDeposit>> balanceUpdate) {
        try {
            Stream.iterate(
                            pageAfter(BEFORE_FIRST_ID),
                            page -> !page.isEmpty(),
                            page -> pageAfter(page.getLast().getId()))
                    .forEach(page -> updatePage(page, balanceUpdate));
        } catch (OptimisticLockException exception) {
            throw new ConcurrentUpdateException("Time deposits were updated concurrently", exception);
        }
    }

    private List<TimeDepositEntity> pageAfter(int id) {
        return repo.findByIdGreaterThanOrderByIdAsc(id, pageSize);
    }

    private void updatePage(List<TimeDepositEntity> page, Consumer<List<TimeDeposit>> balanceUpdate) {
        var entitiesById =
                page.stream().collect(Collectors.toUnmodifiableMap(TimeDepositEntity::getId, Function.identity()));
        var deposits = page.stream().map(mapper::toDomain).toList();

        balanceUpdate.accept(deposits);

        deposits.stream()
                .filter(deposit -> isBalanceChanged(entitiesById.get(deposit.getId()), deposit))
                .forEach(deposit -> mapper.applyBalance(deposit, entitiesById.get(deposit.getId())));

        entityManager.flush();
        entityManager.clear();
    }

    private boolean isBalanceChanged(TimeDepositEntity entity, TimeDeposit deposit) {
        return entity.getBalance().compareTo(MoneyRounding.toCents(deposit.getBalance())) != 0;
    }
}
