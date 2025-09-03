package com.pagerealm.service;

import com.pagerealm.dto.MembershipStatusDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MembershipService {
    void recordPurchase(Long userId, BigDecimal amount, LocalDateTime occurredAt);
    MembershipStatusDTO getStatus(Long userId);
}

