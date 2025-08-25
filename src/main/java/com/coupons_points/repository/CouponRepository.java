package com.coupons_points.repository;

import com.coupons_points.entity.Coupon;
import com.coupons_points.entity.Coupon.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByGenericCodeIgnoreCase(String genericCode);
    Page<Coupon> findAllByStatus(CouponStatus status, Pageable pageable);
}

