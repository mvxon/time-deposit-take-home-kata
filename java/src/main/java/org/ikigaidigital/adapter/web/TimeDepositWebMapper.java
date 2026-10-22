package org.ikigaidigital.adapter.web;

import java.math.BigDecimal;
import java.util.List;
import org.ikigaidigital.adapter.web.dto.TimeDepositDto;
import org.ikigaidigital.adapter.web.dto.WithdrawalDto;
import org.ikigaidigital.domain.model.MoneyRounding;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.model.Withdrawal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface TimeDepositWebMapper {

    List<TimeDepositDto> toDtos(List<TimeDepositDetails> details);

    @Mapping(target = ".", source = "deposit")
    @Mapping(target = "balance", source = "deposit.balance", qualifiedByName = "toMoney")
    TimeDepositDto toDto(TimeDepositDetails details);

    WithdrawalDto toDto(Withdrawal withdrawal);

    @Named("toMoney")
    default BigDecimal toMoney(Double amount) {
        return MoneyRounding.toCents(amount);
    }
}
