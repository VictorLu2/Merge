package com.coupons_points.dto.points;

import com.coupons_points.entity.PointLot;
import com.coupons_points.entity.PointsLedger;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class PointsDtos {

    @Data
    public static class AdjustRequest {
        @NotNull
        private Long userId;
        @NotNull
        @Min(-1_000_000)
        @Max(1_000_000)
        private Integer amount; // 正=加點；負=扣點
        private String note;
    }

    @Data
    public static class EarnRequest {
        @NotNull
        private Long userId;
        @NotNull
        private Long orderId;
        @NotNull
        @Min(0)
        private Integer orderAmount; // 應付金額（以此計算回饋）
        private String note;
    }

    @Data
    public static class RedeemRequest {
        @NotNull
        private Long userId;
        @NotNull
        private Long orderId;
        @NotNull
        @Min(1)
        private Integer redeemPoints; // 要扣的點數
        private String note;
    }

    @Data
    public static class RefundRequest {
        @NotNull
        private Long userId;
        @NotNull
        private Long orderId;
        @NotNull
        @Min(1)
        private Integer points; // 要退回的點數
        private String note;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BalanceResponse {
        private Long userId;
        private Integer balance;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LedgerItem {
        private Long id;
        private Integer changeAmount;
        private PointsLedger.Reason reason;
        private Long relatedOrderId;
        private String note;
        private Integer balanceAfter;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LotItem {
        private Long id;
        private Integer earnedPoints;
        private Integer usedPoints;
        private Integer remaining;
        private PointLot.Source source;
        private Long relatedOrderId;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GenericResponse {
        private String status;
        private String message;
    }
}

