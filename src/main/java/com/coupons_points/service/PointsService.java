package com.coupons_points.service;

import com.coupons_points.dto.points.PointsDtos;
import com.coupons_points.entity.PointLot;
import com.coupons_points.entity.PointRule;
import com.coupons_points.entity.PointsAccount;
import com.coupons_points.entity.PointsLedger;
import com.coupons_points.repository.PointLotRepository;
import com.coupons_points.repository.PointRuleRepository;
import com.coupons_points.repository.PointsAccountRepository;
import com.coupons_points.repository.PointsLedgerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsAccountRepository accountRepo;
    private final PointsLedgerRepository ledgerRepo;
    private final PointLotRepository lotRepo;
    private final PointRuleRepository ruleRepo;

    // Query
    @Transactional(readOnly = true)
    public PointsDtos.BalanceResponse getBalance(Long userId) {
        PointsAccount acc = accountRepo.findById(userId).orElseGet(() -> PointsAccount.builder().userId(userId).balance(0).updatedAt(LocalDateTime.now()).build());
        return PointsDtos.BalanceResponse.builder().userId(userId).balance(acc.getBalance()).updatedAt(acc.getUpdatedAt()).build();
    }

    @Transactional(readOnly = true)
    public Page<PointsDtos.LedgerItem> getLedger(Long userId, Pageable pageable) {
        return ledgerRepo.findAllByUserIdOrderByCreatedAtDesc(userId, pageable).map(l -> PointsDtos.LedgerItem.builder()
                .id(l.getId())
                .changeAmount(l.getChangeAmount())
                .reason(l.getReason())
                .relatedOrderId(l.getRelatedOrderId())
                .note(l.getNote())
                .balanceAfter(l.getBalanceAfter())
                .createdAt(l.getCreatedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<PointsDtos.LotItem> getLots(Long userId, Pageable pageable) {
        return lotRepo.findAllByUserIdOrderByExpiresAtAscIdAsc(userId, pageable).map(l -> PointsDtos.LotItem.builder()
                .id(l.getId())
                .earnedPoints(l.getEarnedPoints())
                .usedPoints(l.getUsedPoints())
                .remaining(l.remaining())
                .source(l.getSource())
                .relatedOrderId(l.getRelatedOrderId())
                .expiresAt(l.getExpiresAt())
                .createdAt(l.getCreatedAt())
                .build());
    }

    // Admin Adjust
    @Transactional
    public PointsDtos.GenericResponse adjust(PointsDtos.AdjustRequest req) {
        if (req.getAmount() == 0) return ok("無變動");
        if (req.getAmount() > 0) {
            addPoints(req.getUserId(), req.getAmount(), PointsLedger.Reason.ADJUSTMENT, null, req.getNote(), PointLot.Source.ADJUSTMENT);
        } else {
            consumePoints(req.getUserId(), -req.getAmount(), PointsLedger.Reason.ADJUSTMENT, null, req.getNote());
        }
        return ok("OK");
    }

    // Earn from order
    @Transactional
    public PointsDtos.GenericResponse earn(PointsDtos.EarnRequest req) {
        PointRule rule = currentRule();
        int points = calcRewardPoints(rule, req.getOrderAmount());
        if (points <= 0) return ok("無回饋");
        LocalDateTime expiresAt = computeExpiry(rule, LocalDateTime.now());
        addPointsWithCustomExpiry(req.getUserId(), points, PointsLedger.Reason.PURCHASE_REWARD, req.getOrderId(), req.getNote(), PointLot.Source.ORDER, expiresAt);
        return ok("earned=" + points);
    }

    // Redeem
    @Transactional
    public PointsDtos.GenericResponse redeem(PointsDtos.RedeemRequest req) {
        consumePoints(req.getUserId(), req.getRedeemPoints(), PointsLedger.Reason.REDEEM, req.getOrderId(), req.getNote());
        return ok("redeemed=" + req.getRedeemPoints());
    }

    // Refund
    @Transactional
    public PointsDtos.GenericResponse refund(PointsDtos.RefundRequest req) {
        PointRule rule = currentRule();
        LocalDateTime expiresAt = computeExpiry(rule, LocalDateTime.now());
        addPointsWithCustomExpiry(req.getUserId(), req.getPoints(), PointsLedger.Reason.REFUND, req.getOrderId(), req.getNote(), PointLot.Source.OTHER, expiresAt);
        return ok("refunded=" + req.getPoints());
    }

    // Rules
    @Transactional(readOnly = true)
    public PointRule currentRule() {
        PointRule rule = ruleRepo.findTopByOrderByIdDesc();
        if (rule == null) {
            rule = PointRule.builder()
                    .rewardRateBp(100) // 1%
                    .redeemRate(BigDecimal.ONE)
                    .expiryPolicy(PointRule.ExpiryPolicy.ROLLING_DAYS)
                    .rollingDays(180)
                    .build();
        }
        return rule;
    }

    @Transactional
    public PointRule upsertRule(PointRule incoming) {
        if (incoming.getRewardRateBp() == null) incoming.setRewardRateBp(100);
        if (incoming.getRedeemRate() == null) incoming.setRedeemRate(BigDecimal.ONE);
        if (incoming.getExpiryPolicy() == null) incoming.setExpiryPolicy(PointRule.ExpiryPolicy.ROLLING_DAYS);
        return ruleRepo.save(incoming);
    }

    // Scheduler: expire lots every night 02:00
    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public void expireLotsCron() {
        LocalDateTime now = LocalDateTime.now();
        List<PointLot> expLots = lotRepo.findByExpiresAtLessThanEqual(now);
        for (PointLot lot : expLots) {
            int remaining = lot.remaining();
            if (remaining <= 0) continue;
            lot.setUsedPoints(lot.getEarnedPoints());
            lotRepo.save(lot);
            PointsAccount acc = getOrCreateAccount(lot.getUserId());
            acc.setBalance(Math.max(0, acc.getBalance() - remaining));
            accountRepo.save(acc);
            ledgerRepo.save(PointsLedger.builder()
                    .userId(lot.getUserId())
                    .changeAmount(-remaining)
                    .reason(PointsLedger.Reason.EXPIRE)
                    .relatedOrderId(null)
                    .note("expire lot #" + lot.getId())
                    .balanceAfter(acc.getBalance())
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    // Internal helpers
    private PointsAccount getOrCreateAccount(Long userId) {
        return accountRepo.findById(userId).orElseGet(() -> accountRepo.save(PointsAccount.builder()
                .userId(userId)
                .balance(0)
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private void addPoints(Long userId, int points, PointsLedger.Reason reason, Long orderId, String note, PointLot.Source source) {
        LocalDateTime expiresAt = computeExpiry(currentRule(), LocalDateTime.now());
        addPointsWithCustomExpiry(userId, points, reason, orderId, note, source, expiresAt);
    }

    private void addPointsWithCustomExpiry(Long userId, int points, PointsLedger.Reason reason, Long orderId, String note, PointLot.Source source, LocalDateTime expiresAt) {
        if (points <= 0) return;
        PointsAccount acc = getOrCreateAccount(userId);
        acc.setBalance(acc.getBalance() + points);
        accountRepo.save(acc);
        ledgerRepo.save(PointsLedger.builder()
                .userId(userId)
                .changeAmount(points)
                .reason(reason)
                .relatedOrderId(orderId)
                .note(note)
                .balanceAfter(acc.getBalance())
                .createdAt(LocalDateTime.now())
                .build());
        lotRepo.save(PointLot.builder()
                .userId(userId)
                .source(source)
                .relatedOrderId(orderId)
                .earnedPoints(points)
                .usedPoints(0)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void consumePoints(Long userId, int points, PointsLedger.Reason reason, Long orderId, String note) {
        if (points <= 0) throw new IllegalArgumentException("points 必須 > 0");
        PointsAccount acc = getOrCreateAccount(userId);
        if (acc.getBalance() < points) throw new IllegalArgumentException("點數不足");

        // FIFO consume lots
        int remainingToConsume = points;
        List<PointLot> lots = lotRepo.findByUserIdOrderByExpiresAtAscIdAsc(userId);
        for (PointLot lot : lots) {
            if (remainingToConsume <= 0) break;
            int lotRem = lot.remaining();
            if (lotRem <= 0) continue;
            int use = Math.min(lotRem, remainingToConsume);
            lot.setUsedPoints(lot.getUsedPoints() + use);
            lotRepo.save(lot);
            remainingToConsume -= use;
        }
        if (remainingToConsume != 0) throw new IllegalStateException("扣點失敗: 批次不足");

        acc.setBalance(acc.getBalance() - points);
        accountRepo.save(acc);
        ledgerRepo.save(PointsLedger.builder()
                .userId(userId)
                .changeAmount(-points)
                .reason(reason)
                .relatedOrderId(orderId)
                .note(note)
                .balanceAfter(acc.getBalance())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private int calcRewardPoints(PointRule rule, int orderAmount) {
        if (orderAmount <= 0) return 0;
        int pts = new BigDecimal(orderAmount)
                .multiply(new BigDecimal(rule.getRewardRateBp()))
                .divide(new BigDecimal(10_000), 0, RoundingMode.FLOOR)
                .intValue();
        if (rule.getMaxRewardPoints() != null) pts = Math.min(pts, rule.getMaxRewardPoints());
        return Math.max(pts, 0);
    }

    private LocalDateTime computeExpiry(PointRule rule, LocalDateTime base) {
        if (rule == null || rule.getExpiryPolicy() == null || rule.getExpiryPolicy() == PointRule.ExpiryPolicy.NONE) return null;
        return switch (rule.getExpiryPolicy()) {
            case ROLLING_DAYS -> base.plusDays(rule.getRollingDays() == null ? 0 : rule.getRollingDays());
            case FIXED_DATE -> {
                // fixed_expire_day: e.g., 1231
                if (rule.getFixedExpireDay() == null) yield null;
                int mmdd = rule.getFixedExpireDay();
                int mm = mmdd / 100; int dd = mmdd % 100;
                LocalDate date = LocalDate.of(base.getYear(), mm, dd);
                if (!date.atStartOfDay().isAfter(base)) {
                    date = LocalDate.of(base.getYear() + 1, mm, dd);
                }
                yield date.atStartOfDay();
            }
            default -> null;
        };
    }

    private PointsDtos.GenericResponse ok(String message) {
        return PointsDtos.GenericResponse.builder().status("OK").message(message).build();
    }
}

