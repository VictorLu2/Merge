package com.pagerealm.service.impl;

import com.pagerealm.dto.MembershipStatusDTO;
import com.pagerealm.entity.User;
import com.pagerealm.entity.MembershipTier;
import com.pagerealm.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class MembershipServiceImpl implements com.pagerealm.service.MembershipService {

    private final UserRepository userRepository;

    public MembershipServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void recordPurchase(Long userId, BigDecimal amount, LocalDateTime occurredAt) {
        Objects.requireNonNull(userId, "userId");
        if (amount == null) amount = BigDecimal.ZERO;
        if (occurredAt == null) occurredAt = LocalDateTime.now();

        User user = userRepository.findById(userId).orElseThrow();

        finalizeWindowIfEnded(user, occurredAt);

        if (user.getMembershipWindowStart() == null) {
            user.setMembershipWindowStart(occurredAt);
            user.setMembershipWindowEnd(occurredAt.plusDays(30).minusSeconds(1));
            user.setMembershipWindowTotal(BigDecimal.ZERO);
        }

        if (!occurredAt.isAfter(user.getMembershipWindowEnd())) {
            user.setMembershipWindowTotal(user.getMembershipWindowTotal().add(amount));
        } else {
            applyLevelUpAndReset(user);
            user.setMembershipWindowStart(occurredAt);
            user.setMembershipWindowEnd(occurredAt.plusDays(30).minusSeconds(1));
            user.setMembershipWindowTotal(amount);
        }

        userRepository.save(user);
    }

    @Override
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

    private void finalizeWindowIfEnded(User user, LocalDateTime now) {
        if (user.getMembershipWindowEnd() != null && now.isAfter(user.getMembershipWindowEnd())) {
            applyLevelUpAndReset(user);
        }
    }
}

