package com.pagerealm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "support_ticket")
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 僅允許登入者建立，關聯為必填
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String contactName;

    @Column(nullable = false, length = 180)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppTicketCategory category;

    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppTicketStatus status = AppTicketStatus.OPEN;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<SupportMessage> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        lastMessageAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
