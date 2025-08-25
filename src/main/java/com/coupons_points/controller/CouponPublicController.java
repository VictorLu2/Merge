package com.coupons_points.controller;

import com.coupons_points.dto.coupon.CouponDtos;
import com.coupons_points.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponPublicController {

    private final CouponService couponService;

    // GET /api/coupons/{code}/validate?user_id=&order_amount=
    @GetMapping("/{code}/validate")
    public CouponDtos.ValidateResponse validate(@PathVariable("code") String code,
                                                @RequestParam("user_id") Long userId,
                                                @RequestParam("order_amount") Integer orderAmount) {
        return couponService.validate(code, userId, orderAmount);
    }

    // POST /api/coupons/{code}/redeem
    @PostMapping("/{code}/redeem")
    public CouponDtos.RedemptionResponse redeem(@PathVariable("code") String code,
                                                @RequestBody @Valid CouponDtos.RedeemRequest req) {
        return couponService.redeem(code, req);
    }

    // GET /api/coupons/redemptions?user_id=
    @GetMapping("/redemptions")
    public Page<CouponDtos.RedemptionResponse> myRedemptions(@RequestParam("user_id") Long userId, Pageable pageable) {
        return couponService.listRedemptionsByUser(userId, pageable);
    }

    // POST /api/coupons/redemptions/{id}/reverse
    @PostMapping("/redemptions/{id}/reverse")
    public CouponDtos.RedemptionResponse reverse(@PathVariable("id") Long redemptionId,
                                                 @RequestParam(value = "note", required = false) String note) {
        return couponService.reverse(redemptionId, note);
    }
}

