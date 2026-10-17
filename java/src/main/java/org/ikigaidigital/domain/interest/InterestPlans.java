package org.ikigaidigital.domain.interest;

import java.util.List;

public final class InterestPlans {

    private static final InterestPlan BASIC = new AnnualRatePlan("basic", 0.01, days -> true);

    // "No interest after 1 year": a year is taken as 365 days, matching the original `days < 366` check.
    private static final InterestPlan STUDENT = new AnnualRatePlan("student", 0.03, days -> days <= 365);

    private static final InterestPlan PREMIUM = new AnnualRatePlan("premium", 0.05, days -> days > 45);

    private InterestPlans() {}

    public static List<InterestPlan> defaults() {
        return List.of(BASIC, STUDENT, PREMIUM);
    }
}
