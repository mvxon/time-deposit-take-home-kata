package org.ikigaidigital.domain.interest;

/**
 * Interest rule of one plan type. Implementations are registered in {@link InterestPlans#defaults()}.
 */
public interface InterestPlan {

    /**
     * The {@code planType} value of the deposits this plan applies to, e.g. {@code "basic"}.
     */
    String planType();

    /**
     * Interest earned for one month, before rounding: the calculator rounds it to cents and adds it to the balance.
     * Called only after the 30-day grace period, which the calculator applies to every plan. Returns {@code 0} when
     * the deposit is not eligible. Must depend only on the given context.
     */
    double monthlyInterest(InterestContext context);
}
