package com.coupons_points.dto.coupon;

import com.coupons_points.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class CouponDtos {

    @Data
    public static class CreateRequest {
        @NotBlank
        private String name;
        @NotNull
        private Coupon.CouponCodeType codeType; // 目前僅支援 GENERIC
        @Size(max = 100)
        private String genericCode;
        @NotNull
        private Coupon.DiscountType discountType;
        @NotNull
        @Min(0)
        private Integer discountValue;
        @Min(0)
        private Integer maxDiscountAmount;
        @Min(0)
        private Integer minSpendAmount;
        @NotNull
        private LocalDateTime startsAt;
        @NotNull
        private LocalDateTime endsAt;
        private Coupon.CouponStatus status = Coupon.CouponStatus.DRAFT;
        @Min(1)
        private Integer totalUsageLimit;
        @Min(1)
        private Integer perUserLimit;
        private Long createdBy;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank
        private String name;
        @NotNull
        private Coupon.DiscountType discountType;
        @NotNull
        @Min(0)
        private Integer discountValue;
        @Min(0)
        private Integer maxDiscountAmount;
        @Min(0)
        private Integer minSpendAmount;
        @NotNull
        private LocalDateTime startsAt;
        @NotNull
        private LocalDateTime endsAt;
        private Coupon.CouponStatus status;
        @Min(1)
        private Integer totalUsageLimit;
        @Min(1)
        private Integer perUserLimit;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CouponResponse {
        private Long id;
        private String name;
        private Coupon.CouponCodeType codeType;
        private String genericCode;
        private Coupon.DiscountType discountType;
        private Integer discountValue;
        private Integer maxDiscountAmount;
        private Integer minSpendAmount;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private Coupon.CouponStatus status;
        private Integer totalUsageLimit;
        private Integer perUserLimit;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidateResponse {
        private boolean valid;
        private String reason; // 若不可用則說明
        private Long couponId;
        private String code;
        private Integer discountAmount; // 建議折扣
    }

    @Data
    public static class RedeemRequest {
        @NotNull
        private Long userId;
        @NotNull
        private Long orderId;
        private Long orderItemId;
        @NotNull
        @Min(0)
        private Integer orderAmount;
        private String note;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RedemptionResponse {
        private Long redemptionId;
        private Long couponId;
        private Long userId;
        private Long orderId;
        private Integer amountDiscounted;
        private String status;
        private String note;
    }
}

