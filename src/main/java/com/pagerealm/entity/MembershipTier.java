package com.pagerealm.entity;

import java.math.BigDecimal;
import java.util.Arrays;

public enum MembershipTier {
    LV1(1, new BigDecimal("0"),    new BigDecimal("0.01")),
    LV2(2, new BigDecimal("500"),  new BigDecimal("0.03")),
    LV3(3, new BigDecimal("2000"), new BigDecimal("0.05")),
    LV4(4, new BigDecimal("5000"), new BigDecimal("0.08")),
    LV5(5, new BigDecimal("10000"),new BigDecimal("0.12"));

    private final int level;
    private final BigDecimal threshold;
    private final BigDecimal cashbackRate; // 0.01 = 1%

    MembershipTier(int level, BigDecimal threshold, BigDecimal cashbackRate) {
        this.level = level;
        this.threshold = threshold;
        this.cashbackRate = cashbackRate;
    }

    public int level() { return level; }
    public BigDecimal threshold() { return threshold; }
    public BigDecimal cashbackRate() { return cashbackRate; }

    public static MembershipTier fromAmount(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        MembershipTier result = LV1;
        for (MembershipTier t : values()) {
            if (amount.compareTo(t.threshold) >= 0 && t.level >= result.level) {
                result = t;
            }
        }
        return result;
    }

    public MembershipTier next() {
        return Arrays.stream(values()).filter(t -> t.level == this.level + 1).findFirst().orElse(null);
    }

    public BigDecimal amountToNext(BigDecimal amount) {
        MembershipTier next = next();
        if (next == null) return BigDecimal.ZERO;
        if (amount == null) amount = BigDecimal.ZERO;
        BigDecimal diff = next.threshold.subtract(amount);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    public String displayName() {
        return "會員等級" + level;
    }
}
