package com.admin_log.controller;

import com.admin_log.entity.AdminLog;
import com.admin_log.service.AdminLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/admin-logs")
public class AdminLogAdminController {

    private final AdminLogService service;

    public AdminLogAdminController(AdminLogService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AdminLog> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public AdminLog get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/by-admin/{adminId}")
    public Page<AdminLog> listByAdmin(@PathVariable Long adminId, @PageableDefault(size = 20) Pageable pageable) {
        return service.listByAdmin(adminId, pageable);
    }

    @PostMapping
    public ResponseEntity<AdminLog> create(@RequestBody AdminLog payload) {
        AdminLog created = service.create(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}

