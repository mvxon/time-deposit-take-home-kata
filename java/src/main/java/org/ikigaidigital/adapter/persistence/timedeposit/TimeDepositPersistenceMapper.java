package org.ikigaidigital.adapter.persistence.timedeposit;

import java.math.BigDecimal;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.MoneyRounding;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.model.Withdrawal;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface TimeDepositPersistenceMapper {

    TimeDeposit toDomain(TimeDepositEntity entity);

    @Mapping(target = "deposit", source = "entity")
    TimeDepositDetails toDetails(TimeDepositEntity entity);

    Withdrawal toDomain(WithdrawalEntity entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "balance", source = "balance", qualifiedByName = "toMoney")
    void applyBalance(TimeDeposit deposit, @MappingTarget TimeDepositEntity entity);

    @Named("toMoney")
    default BigDecimal toMoney(Double amount) {
        return MoneyRounding.toCents(amount);
    }
}
