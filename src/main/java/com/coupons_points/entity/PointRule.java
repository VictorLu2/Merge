package com.coupons_points.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 回饋比率（萬分比），100=1%
    @Column(name = "reward_rate_bp", nullable = false)
    private Integer rewardRateBp;

    // 每筆回饋上限
    @Column(name = "max_reward_points")
    private Integer maxRewardPoints;

    // 1點=多少貨幣單位
    @Column(name = "redeem_rate", nullable = false, precision = 10, scale = 4)
    private java.math.BigDecimal redeemRate;

    // 折抵上限（占應付金額%），例 5000=50%
    @Column(name = "max_redeem_ratio_bp")
    private Integer maxRedeemRatioBp;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_policy", nullable = false, length = 20)
    private ExpiryPolicy expiryPolicy;

    // 例：180 表示獲點後 180 天到期
    @Column(name = "rolling_days")
    private Integer rollingDays;

    // 例：每年 1231 (12/31)，由排程計算到期日
    @Column(name = "fixed_expire_day")
    private Integer fixedExpireDay;

    public enum ExpiryPolicy { NONE, FIXED_DATE, ROLLING_DAYS }
}

