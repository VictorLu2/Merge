package com.coupons_points.repository;

import com.coupons_points.entity.PointsLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {
    Page<PointsLedger> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

