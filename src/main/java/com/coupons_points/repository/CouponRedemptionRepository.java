package com.coupons_points.repository;

import com.coupons_points.entity.CouponRedemption;
import com.coupons_points.entity.CouponRedemption.RedemptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCouponIdAndStatus(Long couponId, RedemptionStatus status);
    long countByCouponIdAndUserIdAndStatus(Long couponId, Long userId, RedemptionStatus status);
    boolean existsByOrderIdAndCouponId(Long orderId, Long couponId);
    Page<CouponRedemption> findAllByUserIdOrderByRedeemedAtDesc(Long userId, Pageable pageable);
}

