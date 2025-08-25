package com.coupons_points.controller;

import com.coupons_points.dto.points.PointsDtos;
import com.coupons_points.entity.PointRule;
import com.coupons_points.service.PointsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    // Query
    @GetMapping("/{userId}/balance")
    public PointsDtos.BalanceResponse balance(@PathVariable Long userId) {
        return pointsService.getBalance(userId);
    }

    @GetMapping("/{userId}/ledger")
    public Page<PointsDtos.LedgerItem> ledger(@PathVariable Long userId, Pageable pageable) {
        return pointsService.getLedger(userId, pageable);
    }

    @GetMapping("/{userId}/lots")
    public Page<PointsDtos.LotItem> lots(@PathVariable Long userId, Pageable pageable) {
        return pointsService.getLots(userId, pageable);
    }

    // Actions
    @PostMapping("/adjust")
    public PointsDtos.GenericResponse adjust(@RequestBody @Valid PointsDtos.AdjustRequest req) {
        return pointsService.adjust(req);
    }

    @PostMapping("/earn")
    public PointsDtos.GenericResponse earn(@RequestBody @Valid PointsDtos.EarnRequest req) {
        return pointsService.earn(req);
    }

    @PostMapping("/redeem")
    public PointsDtos.GenericResponse redeem(@RequestBody @Valid PointsDtos.RedeemRequest req) {
        return pointsService.redeem(req);
    }

    @PostMapping("/refund")
    public PointsDtos.GenericResponse refund(@RequestBody @Valid PointsDtos.RefundRequest req) {
        return pointsService.refund(req);
    }

    // Rules
    @GetMapping("/rules/current")
    public PointRule currentRule() {
        return pointsService.currentRule();
    }

    @PutMapping("/rules")
    public PointRule upsertRule(@RequestBody PointRule rule) {
        return pointsService.upsertRule(rule);
    }
}

