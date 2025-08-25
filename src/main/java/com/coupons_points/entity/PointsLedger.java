package com.coupons_points.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_ledger", indexes = {
        @Index(name = "ix_ledger_user_created", columnList = "user_id,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount; // 正=加點；負=扣點

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private Reason reason;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum Reason { PURCHASE_REWARD, REDEEM, ADJUSTMENT, EXPIRE, REFUND }
}

