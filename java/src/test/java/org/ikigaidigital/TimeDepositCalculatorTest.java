package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.ikigaidigital.domain.interest.AnnualRatePlan;
import org.ikigaidigital.domain.interest.InterestContext;
import org.ikigaidigital.domain.interest.InterestPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeDepositCalculatorTest {

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} plan, {1} days, balance {2} -> {3}")
    @CsvSource({
        "basic,   30,  10000.00, 10000.00",
        "basic,   31,  10000.00, 10008.33",
        "basic,   45,  1234567.00, 1235595.81",
        "basic,   1000, 10000.00, 10008.33",
        "student, 30,  10000.00, 10000.00",
        "student, 31,  10000.00, 10025.00",
        "student, 365, 10000.00, 10025.00",
        "student, 366, 10000.00, 10000.00",
        "premium, 30,  10000.00, 10000.00",
        "premium, 31,  10000.00, 10000.00",
        "premium, 45,  10000.00, 10000.00",
        "premium, 46,  10000.00, 10041.67",
        "premium, 1000, 10000.00, 10041.67",
        "unknown, 100, 10000.00, 10000.00",
        "basic,   100, 0.00,     0.00",
        "basic,   100, 123.45,   123.55",
        "premium, 100, 999.99,   1004.16"
    })
    void appliesMonthlyInterestByPlanType(String planType, int days, double balance, double expectedBalance) {
        var deposit = new TimeDeposit(1, planType, balance, days);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
    }

    @ParameterizedTest(name = "{0} plan, balance {1} -> {2} (BigDecimal.valueOf would round up)")
    @CsvSource({
        "basic,   18.00, 18.01",
        "basic,   90.00, 90.07",
        "student, 6.00,  6.01",
        "student, 30.00, 30.07",
        "premium, 18.00, 18.07",
        "premium, 25.20, 25.30"
    })
    void keepsLegacyRoundingOfBinaryInterestValue(String planType, double balance, double expectedBalance) {
        var deposit = new TimeDeposit(1, planType, balance, 100);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void updatesEveryDepositInTheListIndependently() {
        var deposits = List.of(
                new TimeDeposit(1, "basic", 10000.00, 31),
                new TimeDeposit(2, "student", 10000.00, 366),
                new TimeDeposit(3, "premium", 10000.00, 46));

        calculator.updateBalance(deposits);

        assertThat(deposits).extracting(TimeDeposit::getBalance).containsExactly(10008.33, 10000.00, 10041.67);
    }

    @Test
    void keepsIdentityPlanTypeAndDaysUnchanged() {
        var deposit = new TimeDeposit(7, "premium", 10000.00, 46);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit)
                .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getDays)
                .containsExactly(7, "premium", 46);
    }

    @Test
    void appliesInterestOfAdditionallyRegisteredAnnualRatePlan() {
        var goldPlan = new AnnualRatePlan("gold", 0.12, days -> true);
        var deposit = new TimeDeposit(1, "gold", 10000.00, 31);

        new TimeDepositCalculator(List.of(goldPlan)).updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(10100.00);
    }

    @Test
    void appliesGracePeriodToAdditionallyRegisteredPlans() {
        var goldPlan = new AnnualRatePlan("gold", 0.12, days -> true);
        var deposit = new TimeDeposit(1, "gold", 10000.00, 30);

        new TimeDepositCalculator(List.of(goldPlan)).updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(10000.00);
    }

    @Test
    void appliesInterestOfPlanWithNonLinearRule() {
        var tieredPlan = new InterestPlan() {
            @Override
            public String planType() {
                return "tiered";
            }

            @Override
            public double monthlyInterest(InterestContext context) {
                return Math.min(context.balance(), 10000.00) * 0.02 / 12
                        + Math.max(context.balance() - 10000.00, 0) * 0.04 / 12;
            }
        };
        var deposit = new TimeDeposit(1, "tiered", 16000.00, 100);

        new TimeDepositCalculator(List.of(tieredPlan)).updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(16036.67);
    }

    @Test
    void keepsDepositWithoutPlanTypeUnchangedDuringGracePeriod() {
        var deposit = new TimeDeposit(1, null, 10000.00, 10);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(10000.00);
    }

    @Test
    void failsOnDepositWithoutPlanTypeAfterGracePeriodLikeTheOriginal() {
        var deposits = List.of(
                new TimeDeposit(1, "basic", 10000.00, 31),
                new TimeDeposit(2, null, 10000.00, 31),
                new TimeDeposit(3, "basic", 10000.00, 31));

        assertThatThrownBy(() -> calculator.updateBalance(deposits)).isInstanceOf(NullPointerException.class);
        assertThat(deposits).extracting(TimeDeposit::getBalance).containsExactly(10008.33, 10000.00, 10000.00);
    }

    @Test
    void acceptsEmptyList() {
        var deposits = new ArrayList<TimeDeposit>();

        calculator.updateBalance(deposits);

        assertThat(deposits).isEmpty();
    }
}
