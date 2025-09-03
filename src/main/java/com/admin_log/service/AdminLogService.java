package com.admin_log.service;

import com.admin_log.entity.AdminLog;
import com.admin_log.repository.AdminLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class AdminLogService {

    private final AdminLogRepository repository;

    public AdminLogService(AdminLogRepository repository) {
        this.repository = repository;
    }

    public Page<AdminLog> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<AdminLog> listByAdmin(Long adminId, Pageable pageable) {
        return repository.findByAdminId(adminId, pageable);
    }

    public AdminLog get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("AdminLog not found: " + id));
    }

    @Transactional
    public AdminLog create(AdminLog log) {
        log.setId(null);
        return repository.save(log);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("AdminLog not found: " + id);
        }
        repository.deleteById(id);
    }
}

