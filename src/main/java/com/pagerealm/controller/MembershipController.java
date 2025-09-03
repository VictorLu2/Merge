package com.pagerealm.controller;

import com.pagerealm.dto.MembershipStatusDTO;
import com.pagerealm.entity.User;
import com.pagerealm.repository.UserRepository;
import com.pagerealm.service.MembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final UserRepository userRepository;

    public MembershipController(MembershipService membershipService, UserRepository userRepository) {
        this.membershipService = membershipService;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<MembershipStatusDTO> status(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(membershipService.getStatus(user.getUserId()));
    }

    // 給他組整合或本地測試用：紀錄一筆消費
    @PostMapping("/purchase")
    public ResponseEntity<Void> recordPurchase(Principal principal,
                                               @RequestParam("amount") BigDecimal amount) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        membershipService.recordPurchase(user.getUserId(), amount, LocalDateTime.now());
        return ResponseEntity.ok().build();
    }
}
