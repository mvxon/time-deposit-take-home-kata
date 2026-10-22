package org.ikigaidigital.adapter.persistence.timedeposit;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

    List<TimeDepositEntity> findByIdGreaterThanOrderByIdAsc(Integer id, Limit limit);

    @Query("select d from TimeDepositEntity d left join fetch d.withdrawals order by d.id")
    List<TimeDepositEntity> findAllWithWithdrawals();
}
