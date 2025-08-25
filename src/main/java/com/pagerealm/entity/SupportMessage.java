package com.pagerealm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "support_message")
public class SupportMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AppSenderType sender; // USER 或 AGENT

    @Lob
    @Column(nullable = false)
    private String content;

    // 若 sender=USER，記錄使用者ID；若為AGENT，可記錄管理員使用者ID（若有）。
    private Long senderUserId;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
