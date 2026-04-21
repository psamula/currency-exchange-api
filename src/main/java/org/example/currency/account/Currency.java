package org.example.currency.account;

import java.math.RoundingMode;

public enum Currency {
    PLN,
    USD;

    public static final int MONETARY_SCALE = 2;
    public static final RoundingMode MONETARY_ROUNDING = RoundingMode.HALF_EVEN;
}
