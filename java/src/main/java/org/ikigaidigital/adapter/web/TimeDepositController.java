package org.ikigaidigital.adapter.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ikigaidigital.adapter.web.api.TimeDepositsApi;
import org.ikigaidigital.adapter.web.dto.TimeDepositDto;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class TimeDepositController implements TimeDepositsApi {

    private final TimeDepositService service;
    private final TimeDepositWebMapper mapper;

    @Override
    public ResponseEntity<List<TimeDepositDto>> getAllTimeDeposits() {
        return ResponseEntity.ok(mapper.toDtos(service.getAll()));
    }

    @Override
    public ResponseEntity<Void> updateAllBalances() {
        service.applyMonthlyInterest();

        return ResponseEntity.noContent().build();
    }
}
