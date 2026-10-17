package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ikigaidigital.domain.interest.GracePeriod;
import org.ikigaidigital.domain.interest.InterestContext;
import org.ikigaidigital.domain.interest.InterestPlan;
import org.ikigaidigital.domain.interest.InterestPlans;

public class TimeDepositCalculator {

    private static final int CENTS_SCALE = 2;

    private final Map<String, InterestPlan> plansByType;

    public TimeDepositCalculator() {
        this(InterestPlans.defaults());
    }

    public TimeDepositCalculator(Collection<? extends InterestPlan> plans) {
        this.plansByType =
                plans.stream().collect(Collectors.toUnmodifiableMap(InterestPlan::planType, Function.identity()));
    }

    public void updateBalance(List<TimeDeposit> deposits) {
        deposits.forEach(this::applyMonthlyInterest);
    }

    private void applyMonthlyInterest(TimeDeposit deposit) {
        deposit.setBalance(deposit.getBalance() + roundToCents(monthlyInterest(deposit)));
    }

    // As in the original, the grace period is checked before the plan type and applies to every plan: unknown plan
    // types earn no interest, and a missing plan type fails only after the grace period.
    private double monthlyInterest(TimeDeposit deposit) {
        return GracePeriod.isOver(deposit.getDays()) ? planInterest(deposit) : 0.0;
    }

    private double planInterest(TimeDeposit deposit) {
        return Optional.ofNullable(plansByType.get(Objects.requireNonNull(deposit.getPlanType())))
                .map(plan -> plan.monthlyInterest(new InterestContext(deposit.getBalance(), deposit.getDays())))
                .orElse(0.0);
    }

    private static double roundToCents(double amount) {
        return new BigDecimal(amount)
                .setScale(CENTS_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
