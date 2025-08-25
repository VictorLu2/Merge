package com.coupons_points.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsAccount {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private Integer balance;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (balance == null) balance = 0;
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

