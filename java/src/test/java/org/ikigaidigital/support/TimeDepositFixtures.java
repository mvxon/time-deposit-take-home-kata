package org.ikigaidigital.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeDepositFixtures {

    private final JdbcClient jdbc;

    public void clear() {
        jdbc.sql("truncate table withdrawals, time_deposits restart identity").update();
    }

    public void deposit(int id, String planType, int days, String balance) {
        jdbc.sql("insert into time_deposits (id, plan_type, days, balance) values (?, ?, ?, ?)")
                .params(id, planType, days, new BigDecimal(balance))
                .update();
    }

    public void withdrawal(int id, int timeDepositId, String amount, LocalDate date) {
        jdbc.sql("insert into withdrawals (id, time_deposit_id, amount, date) values (?, ?, ?, ?)")
                .params(id, timeDepositId, new BigDecimal(amount), date)
                .update();
    }

    public void changeConcurrently(int id) {
        jdbc.sql("update time_deposits set balance = balance + 1, version = version + 1 where id = ?")
                .param(id)
                .update();
    }

    public int versionOf(int id) {
        return jdbc.sql("select version from time_deposits where id = ?")
                .param(id)
                .query(Integer.class)
                .single();
    }

    public BigDecimal balanceOf(int id) {
        return jdbc.sql("select balance from time_deposits where id = ?")
                .param(id)
                .query(BigDecimal.class)
                .single();
    }
}
