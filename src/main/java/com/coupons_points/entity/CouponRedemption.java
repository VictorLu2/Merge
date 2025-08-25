package com.coupons_points.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_redemptions",
        indexes = {
                @Index(name = "idx_redemptions_user", columnList = "user_id"),
                @Index(name = "idx_redemptions_coupon", columnList = "coupon_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_redemption_order_coupon", columnNames = {"order_id", "coupon_id"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "code_id")
    private Long codeId; // 保留欄位

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    @Column(name = "amount_discounted", nullable = false)
    private Integer amountDiscounted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RedemptionStatus status = RedemptionStatus.APPLIED;

    @Column(name = "note", length = 255)
    private String note;

    @PrePersist
    public void prePersist() {
        if (redeemedAt == null) redeemedAt = LocalDateTime.now();
    }

    public enum RedemptionStatus { APPLIED, REVERSED }
}

