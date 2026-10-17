package org.ikigaidigital.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.ikigaidigital.support.IntegrationTest;
import org.ikigaidigital.support.TimeDepositFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class TimeDepositApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TimeDepositFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clear();
    }

    @Test
    void returnsAllTimeDepositsWithTheirWithdrawals() throws Exception {
        fixtures.deposit(1, "basic", 45, "1234567.00");
        fixtures.deposit(2, "premium", 10, "500.00");
        fixtures.withdrawal(1, 1, "1000.00", LocalDate.of(2026, 1, 15));
        fixtures.withdrawal(2, 1, "250.50", LocalDate.of(2026, 2, 10));

        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].planType").value("basic"))
                .andExpect(jsonPath("$[0].balance").value(1234567.00))
                .andExpect(jsonPath("$[0].days").value(45))
                .andExpect(jsonPath("$[0].withdrawals", hasSize(2)))
                .andExpect(jsonPath("$[0].withdrawals[0].id").value(1))
                .andExpect(jsonPath("$[0].withdrawals[0].amount").value(1000.00))
                .andExpect(jsonPath("$[0].withdrawals[0].date").value("2026-01-15"))
                .andExpect(jsonPath("$[0].withdrawals[1].amount").value(250.50))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].withdrawals", empty()));
    }

    @Test
    void returnsEmptyListWhenThereAreNoTimeDeposits() throws Exception {
        mockMvc.perform(get("/time-deposits")).andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
    }

    @Test
    void appliesMonthlyInterestToEveryTimeDeposit() throws Exception {
        fixtures.deposit(1, "basic", 45, "1234567.00");
        fixtures.deposit(2, "student", 366, "10000.00");
        fixtures.deposit(3, "premium", 46, "10000.00");
        fixtures.deposit(4, "premium", 30, "10000.00");

        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        assertThat(fixtures.balanceOf(1)).isEqualByComparingTo(new BigDecimal("1235595.81"));
        assertThat(fixtures.balanceOf(2)).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(fixtures.balanceOf(3)).isEqualByComparingTo(new BigDecimal("10041.67"));
        assertThat(fixtures.balanceOf(4)).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void appliesInterestToDepositsOnEveryPage() throws Exception {
        fixtures.deposit(10, "basic", 45, "10000.00");
        fixtures.deposit(20, "basic", 45, "10000.00");
        fixtures.deposit(30, "basic", 45, "10000.00");
        fixtures.deposit(40, "basic", 45, "10000.00");
        fixtures.deposit(50, "basic", 45, "10000.00");

        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        assertThat(List.of(10, 20, 30, 40, 50))
                .map(fixtures::balanceOf)
                .allSatisfy(balance -> assertThat(balance).isEqualByComparingTo(new BigDecimal("10008.33")));
    }

    @Test
    void servesClientsThatAcceptOnlyJson() throws Exception {
        fixtures.deposit(1, "basic", 45, "10000.00");

        mockMvc.perform(post("/time-deposits/balance-updates").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/time-deposits").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(10008.33));
    }

    @Test
    void keepsLargestSupportedBalanceExactWhenNoInterestIsDue() throws Exception {
        fixtures.deposit(1, "basic", 10, "9999999999999.99");

        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        assertThat(fixtures.balanceOf(1)).isEqualByComparingTo(new BigDecimal("9999999999999.99"));
    }

    @Test
    void exposesUpdatedBalancesThroughGetEndpoint() throws Exception {
        fixtures.deposit(1, "student", 100, "10000.00");

        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(10025.00))
                .andExpect(jsonPath("$[0].days").value(100));
    }
}
