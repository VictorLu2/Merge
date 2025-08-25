package com.coupons_points.repository;

import com.coupons_points.entity.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
}

