package com.books.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(name = "publisher_name", length = 255)
    private String publisherName;

    @Column(name = "list_price", nullable = false)
    private Integer listPrice;

    @Column(length = 255)
    private String format;

    @Column(length = 255)
    private String status;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(length = 255)
    private String language;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.author == null || this.author.isBlank()) this.author = "作者";
        if (this.status == null || this.status.isBlank()) this.status = "販售中";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

