package com.pagerealm.service;

import com.pagerealm.dto.response.MembershipStatusDTO;
import com.pagerealm.entity.User;
import com.pagerealm.entity.MembershipTier;
import com.pagerealm.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class MembershipService {

    private final UserRepository userRepository;

    public MembershipService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 由訂單/金流成功後呼叫
    @Transactional
    public void recordPurchase(Long userId, BigDecimal amount, LocalDateTime occurredAt) {
        Objects.requireNonNull(userId, "userId");
        if (amount == null) amount = BigDecimal.ZERO;
        if (occurredAt == null) occurredAt = LocalDateTime.now();

        User user = userRepository.findById(userId).orElseThrow();

        // 若視窗已結束，先結算上一期
        finalizeWindowIfEnded(user, occurredAt);

        // 尚未開窗 -> 以本次消費時間當起點，30 天視窗
        if (user.getMembershipWindowStart() == null) {
            user.setMembershipWindowStart(occurredAt);
            user.setMembershipWindowEnd(occurredAt.plusDays(30).minusSeconds(1));
            user.setMembershipWindowTotal(BigDecimal.ZERO);
        }

        // 視窗內累積金額
        if (!occurredAt.isAfter(user.getMembershipWindowEnd())) {
            user.setMembershipWindowTotal(user.getMembershipWindowTotal().add(amount));
        } else {
            // 若消費已跨期，先結算上一期再開新視窗並計入本次金額
            applyLevelUpAndReset(user);
            user.setMembershipWindowStart(occurredAt);
            user.setMembershipWindowEnd(occurredAt.plusDays(30).minusSeconds(1));
            user.setMembershipWindowTotal(amount);
        }

        userRepository.save(user);
    }

    // 讀取狀態（同時做期滿結算）
    @Transactional
    public MembershipStatusDTO getStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        finalizeWindowIfEnded(user, LocalDateTime.now());
        user = userRepository.save(user);

        MembershipStatusDTO dto = new MembershipStatusDTO();
        dto.currentLevel = user.getMembershipTier().level();
        dto.currentTierName = user.getMembershipTier().displayName();
        dto.cashbackRate = user.getMembershipTier().cashbackRate();

        dto.windowStart = user.getMembershipWindowStart();
        dto.windowEnd = user.getMembershipWindowEnd();
        dto.windowTotal = user.getMembershipWindowTotal();

        MembershipTier next = user.getMembershipTier().next();
        if (next != null) {
            dto.nextLevel = next.level();
            dto.nextTierName = next.displayName();
            dto.nextCashbackRate = next.cashbackRate();
            BigDecimal base = user.getMembershipWindowTotal() == null ? BigDecimal.ZERO : user.getMembershipWindowTotal();
            dto.amountToNext = next.threshold().subtract(base).max(BigDecimal.ZERO);
        } else {
            dto.nextLevel = null;
            dto.nextTierName = null;
            dto.nextCashbackRate = null;
            dto.amountToNext = BigDecimal.ZERO;
        }
        return dto;
    }

    // 期滿時升等（只升不降），並重置視窗
    private void applyLevelUpAndReset(User user) {
        BigDecimal total = user.getMembershipWindowTotal() == null ? BigDecimal.ZERO : user.getMembershipWindowTotal();
        MembershipTier target = MembershipTier.fromAmount(total);
        if (target.level() > user.getMembershipTier().level()) {
            user.setMembershipTier(target);
        }
        user.setMembershipWindowStart(null);
        user.setMembershipWindowEnd(null);
        user.setMembershipWindowTotal(BigDecimal.ZERO);
    }

    // 若 now 超過 windowEnd -> 結算升等
    private void finalizeWindowIfEnded(User user, LocalDateTime now) {
        if (user.getMembershipWindowEnd() != null && now.isAfter(user.getMembershipWindowEnd())) {
            applyLevelUpAndReset(user);
        }
    }
}
