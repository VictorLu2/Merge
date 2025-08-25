package com.coupons_points.repository;

import com.coupons_points.entity.PointLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PointLotRepository extends JpaRepository<PointLot, Long> {
    List<PointLot> findByUserIdOrderByExpiresAtAscIdAsc(Long userId);
    Page<PointLot> findAllByUserIdOrderByExpiresAtAscIdAsc(Long userId, Pageable pageable);
    List<PointLot> findByUserIdAndExpiresAtLessThanEqual(Long userId, LocalDateTime cutoff);
    List<PointLot> findByExpiresAtLessThanEqual(LocalDateTime cutoff);
}

