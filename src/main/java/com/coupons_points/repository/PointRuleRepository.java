package com.coupons_points.repository;

import com.coupons_points.entity.PointRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRuleRepository extends JpaRepository<PointRule, Long> {
    PointRule findTopByOrderByIdDesc();
}

