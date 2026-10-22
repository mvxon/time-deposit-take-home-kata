package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TimeDepositCalculatorEquivalenceTest {

    private static final int CASES = 100_000;
    private static final List<String> PLAN_TYPES = List.of("basic", "student", "premium", "unknown");

    private final Random random = new Random(20261017);

    @Test
    void producesTheSameOutcomesAsTheOriginalCalculator() {
        var mismatches = IntStream.range(0, CASES)
                .mapToObj(index -> randomDeposit())
                .filter(this::isDifferentFromOriginal)
                .toList();

        assertThat(mismatches).isEmpty();
    }

    private boolean isDifferentFromOriginal(DepositInput input) {
        return !outcomeOf(new LegacyTimeDepositCalculator()::updateBalance, input)
                .equals(outcomeOf(new TimeDepositCalculator()::updateBalance, input));
    }

    private Outcome outcomeOf(Consumer<List<TimeDeposit>> calculator, DepositInput input) {
        var deposit = input.toDeposit();
        var failure = catchThrowable(() -> calculator.accept(List.of(deposit)));
        return new Outcome(deposit.getBalance(), failure == null ? null : failure.getClass());
    }

    private DepositInput randomDeposit() {
        var planType = randomPlanType();
        var days = random.nextInt(800);
        var balance = random.nextBoolean() ? random.nextInt(10_000_000) / 100.0 : random.nextDouble() * 1_000_000_000;
        return new DepositInput(planType, balance, days);
    }

    private String randomPlanType() {
        var index = random.nextInt(PLAN_TYPES.size() + 1);
        return index < PLAN_TYPES.size() ? PLAN_TYPES.get(index) : null;
    }

    private record DepositInput(String planType, double balance, int days) {

        TimeDeposit toDeposit() {
            return new TimeDeposit(1, planType, balance, days);
        }
    }

    private record Outcome(Double balance, Class<?> failure) {}
}
