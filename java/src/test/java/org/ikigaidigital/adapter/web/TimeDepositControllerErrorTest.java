package org.ikigaidigital.adapter.web;

import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.ikigaidigital.application.TimeDepositService;
import org.ikigaidigital.domain.port.ConcurrentUpdateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TimeDepositController.class)
@Import(TimeDepositWebMapperImpl.class)
class TimeDepositControllerErrorTest {

    private static final String BALANCE_UPDATES = "/time-deposits/balance-updates";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeDepositService service;

    @Test
    void returnsConflictWhenBalancesWereUpdatedConcurrently() throws Exception {
        willThrow(new ConcurrentUpdateException("stale", null)).given(service).applyMonthlyInterest();

        mockMvc.perform(post(BALANCE_UPDATES).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.instance").value(BALANCE_UPDATES));
    }

    @Test
    void hidesDetailsOfUnexpectedErrors() throws Exception {
        willThrow(new IllegalStateException("database password is secret"))
                .given(service)
                .applyMonthlyInterest();

        mockMvc.perform(post(BALANCE_UPDATES))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }

    @Test
    void keepsSpringMvcStatusForFrameworkErrors() throws Exception {
        mockMvc.perform(delete("/time-deposits"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }
}
