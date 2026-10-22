package org.ikigaidigital.domain.interest;

public final class GracePeriod {

    private static final int DAYS = 30;

    private GracePeriod() {}

    public static boolean isOver(int days) {
        return days > DAYS;
    }
}
