package com.books.service;

import com.books.entity.Book;
import com.books.repository.BookRepository;
import com.pagerealm.s3.S3Buckets;
import com.pagerealm.s3.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final S3Service s3Service;
    private final S3Buckets s3Buckets;

    public BookService(BookRepository bookRepository, S3Service s3Service, S3Buckets s3Buckets) {
        this.bookRepository = bookRepository;
        this.s3Service = s3Service;
        this.s3Buckets = s3Buckets;
    }

    public Page<Book> list(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public Book get(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Book not found: " + id));
    }

    @Transactional
    public Book create(Book book) {
        book.setId(null);
        return bookRepository.save(book);
    }

    @Transactional
    public Book update(Long id, Book payload) {
        Book existing = get(id);
        existing.setTitle(payload.getTitle());
        existing.setAuthor(payload.getAuthor());
        existing.setPublisherName(payload.getPublisherName());
        existing.setListPrice(payload.getListPrice());
        existing.setFormat(payload.getFormat());
        existing.setStatus(payload.getStatus());
        existing.setCoverImageUrl(payload.getCoverImageUrl());
        existing.setLanguage(payload.getLanguage());
        existing.setPublishedAt(payload.getPublishedAt());
        // updatedAt will be handled by @PreUpdate
        return bookRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new NoSuchElementException("Book not found: " + id);
        }
        bookRepository.deleteById(id);
    }

    /**
     * 上傳書籍封面，改用 S3 儲存並回傳公開 URL
     */
    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("檔案不可為空");
        }

        String bucket = s3Buckets.getBooks();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket 'aws.s3.buckets.books' 未設定");
        }

        String contentType = file.getContentType();
        String ext = null;
        if (contentType != null) {
            switch (contentType.toLowerCase()) {
                case "image/jpeg", "image/jpg", "image/pjpeg" -> ext = "jpg";
                case "image/png", "image/x-png" -> ext = "png";
                case "image/gif" -> ext = "gif";
                case "image/webp" -> ext = "webp";
            }
        }
        if (ext == null) {
            String name = file.getOriginalFilename();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ext = "jpg";
                else if (lower.endsWith(".png")) ext = "png";
                else if (lower.endsWith(".gif")) ext = "gif";
                else if (lower.endsWith(".webp")) ext = "webp";
            }
        }
        if (ext == null) {
            throw new IllegalArgumentException("不支援的檔案格式");
        }
        if (contentType == null) {
            contentType = switch (ext) {
                case "jpg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                default -> "application/octet-stream";
            };
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String filename = "book_cover_" + ts + "." + ext;
        String key = "books/covers/" + filename;

        try {
            byte[] bytes = file.getBytes();
            s3Service.putObjectWithContentType(bucket, key, bytes, contentType);
            return s3Service.buildPublicUrl(bucket, key);
        } catch (IOException e) {
            throw new RuntimeException("上傳失敗，請稍後再試", e);
        }
    }

    // 供 Controller 在上傳後把 URL 寫回書籍
    @Transactional
    public Book updateCover(Long id, String coverUrl) {
        Book book = get(id);
        book.setCoverImageUrl(coverUrl);
        return bookRepository.save(book);
    }
}
