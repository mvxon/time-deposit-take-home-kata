package org.ikigaidigital.domain.interest;

import java.util.function.IntPredicate;

public record AnnualRatePlan(String planType, double annualRate, IntPredicate eligibleOnDay) implements InterestPlan {

    private static final int MONTHS_IN_YEAR = 12;

    @Override
    public double monthlyInterest(InterestContext context) {
        return eligibleOnDay.test(context.days()) ? context.balance() * annualRate / MONTHS_IN_YEAR : 0;
    }
}
