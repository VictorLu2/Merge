package com.coupons_points.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupons_time", columnList = "starts_at,ends_at"),
        @Index(name = "idx_coupons_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_coupons_generic_code", columnNames = {"generic_code"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_type", nullable = false, length = 20)
    private CouponCodeType codeType;

    @Column(name = "generic_code", length = 100)
    private String genericCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "max_discount_amount")
    private Integer maxDiscountAmount;

    @Column(name = "min_spend_amount")
    private Integer minSpendAmount;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.DRAFT;

    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CouponCodeType { GENERIC, UNIQUE }
    public enum DiscountType { PERCENT, FIXED }
    public enum CouponStatus { DRAFT, ACTIVE, PAUSED, EXPIRED }
}

