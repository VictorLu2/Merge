package com.coupons_points.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_lots", indexes = {
        @Index(name = "ix_lots_user_exp", columnList = "user_id,expires_at,id"),
        @Index(name = "ix_lots_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private Source source;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "earned_points", nullable = false)
    private Integer earnedPoints;

    @Column(name = "used_points", nullable = false)
    private Integer usedPoints;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // NULL=不會到期

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (usedPoints == null) usedPoints = 0;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public int remaining() { return (earnedPoints == null ? 0 : earnedPoints) - (usedPoints == null ? 0 : usedPoints); }

    public enum Source { ORDER, ADJUSTMENT, OTHER }
}

