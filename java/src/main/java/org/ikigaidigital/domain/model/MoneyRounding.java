package org.ikigaidigital.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyRounding {

    private static final int SCALE = 2;

    private MoneyRounding() {}

    public static BigDecimal toCents(double amount) {
        return BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
