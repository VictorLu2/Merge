package com.coupons_points.service;

import com.coupons_points.dto.coupon.CouponDtos;
import com.coupons_points.entity.Coupon;
import com.coupons_points.entity.CouponRedemption;
import com.coupons_points.repository.CouponRedemptionRepository;
import com.coupons_points.repository.CouponRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;

    // Admin
    @Transactional
    public CouponDtos.CouponResponse create(CouponDtos.CreateRequest req) {
        if (req.getCodeType() == Coupon.CouponCodeType.GENERIC) {
            if (req.getGenericCode() == null || req.getGenericCode().isBlank()) {
                throw new IllegalArgumentException("generic_code 必填");
            }
            couponRepository.findByGenericCodeIgnoreCase(req.getGenericCode()).ifPresent(c -> {
                throw new IllegalArgumentException("generic_code 已存在");
            });
        }
        Coupon entity = Coupon.builder()
                .name(req.getName())
                .codeType(req.getCodeType())
                .genericCode(req.getGenericCode())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .minSpendAmount(req.getMinSpendAmount())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .status(req.getStatus() == null ? Coupon.CouponStatus.DRAFT : req.getStatus())
                .totalUsageLimit(req.getTotalUsageLimit())
                .perUserLimit(req.getPerUserLimit())
                .createdBy(req.getCreatedBy())
                .build();
        entity = couponRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public CouponDtos.CouponResponse update(Long id, CouponDtos.UpdateRequest req) {
        Coupon c = couponRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("coupon 不存在"));
        c.setName(req.getName());
        c.setDiscountType(req.getDiscountType());
        c.setDiscountValue(req.getDiscountValue());
        c.setMaxDiscountAmount(req.getMaxDiscountAmount());
        c.setMinSpendAmount(req.getMinSpendAmount());
        c.setStartsAt(req.getStartsAt());
        c.setEndsAt(req.getEndsAt());
        if (req.getStatus() != null) c.setStatus(req.getStatus());
        c.setTotalUsageLimit(req.getTotalUsageLimit());
        c.setPerUserLimit(req.getPerUserLimit());
        return toResponse(couponRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        couponRepository.deleteById(id);
    }

    public Page<CouponDtos.CouponResponse> listByStatus(Coupon.CouponStatus status, Pageable pageable) {
        Page<Coupon> page = status == null ? couponRepository.findAll(pageable) : couponRepository.findAllByStatus(status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public CouponDtos.CouponResponse changeStatus(Long id, Coupon.CouponStatus status) {
        Coupon c = couponRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("coupon 不存在"));
        c.setStatus(status);
        return toResponse(couponRepository.save(c));
    }

    // Public
    public CouponDtos.ValidateResponse validate(String code, Long userId, int orderAmount) {
        Optional<Coupon> opt = couponRepository.findByGenericCodeIgnoreCase(code);
        if (opt.isEmpty()) {
            return CouponDtos.ValidateResponse.builder().valid(false).reason("找不到券").code(code).build();
        }
        Coupon c = opt.get();
        LocalDateTime now = LocalDateTime.now();
        if (c.getStatus() != Coupon.CouponStatus.ACTIVE) {
            return invalid("券未啟用", c, code);
        }
        if (now.isBefore(c.getStartsAt()) || now.isAfter(c.getEndsAt())) {
            return invalid("不在有效期", c, code);
        }
        if (c.getMinSpendAmount() != null && orderAmount < c.getMinSpendAmount()) {
            return invalid("未達最低消費", c, code);
        }
        if (c.getTotalUsageLimit() != null) {
            long used = redemptionRepository.countByCouponIdAndStatus(c.getId(), CouponRedemption.RedemptionStatus.APPLIED);
            if (used >= c.getTotalUsageLimit()) {
                return invalid("已達總使用上限", c, code);
            }
        }
        if (c.getPerUserLimit() != null) {
            long usedByUser = redemptionRepository.countByCouponIdAndUserIdAndStatus(c.getId(), userId, CouponRedemption.RedemptionStatus.APPLIED);
            if (usedByUser >= c.getPerUserLimit()) {
                return invalid("已達個人使用上限", c, code);
            }
        }
        int discount = calculateDiscount(c, orderAmount);
        if (discount <= 0) {
            return invalid("不符合折扣條件", c, code);
        }
        return CouponDtos.ValidateResponse.builder()
                .valid(true)
                .couponId(c.getId())
                .code(code)
                .discountAmount(discount)
                .build();
    }

    @Transactional
    public CouponDtos.RedemptionResponse redeem(String code, CouponDtos.RedeemRequest req) {
        CouponDtos.ValidateResponse vr = validate(code, req.getUserId(), req.getOrderAmount());
        if (!vr.isValid()) {
            throw new IllegalArgumentException("不可用: " + vr.getReason());
        }
        CouponRedemption red = CouponRedemption.builder()
                .couponId(vr.getCouponId())
                .userId(req.getUserId())
                .orderId(req.getOrderId())
                .orderItemId(req.getOrderItemId())
                .amountDiscounted(vr.getDiscountAmount())
                .status(CouponRedemption.RedemptionStatus.APPLIED)
                .note(req.getNote())
                .build();
        try {
            red = redemptionRepository.save(red);
        } catch (DataIntegrityViolationException e) {
            // 可能重複核銷同一張訂單
            throw new IllegalStateException("該訂單已使用此券");
        }
        return CouponDtos.RedemptionResponse.builder()
                .redemptionId(red.getId())
                .couponId(red.getCouponId())
                .userId(red.getUserId())
                .orderId(red.getOrderId())
                .amountDiscounted(red.getAmountDiscounted())
                .status(red.getStatus().name())
                .note(red.getNote())
                .build();
    }

    @Transactional
    public CouponDtos.RedemptionResponse reverse(Long redemptionId, String note) {
        CouponRedemption red = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new EntityNotFoundException("redemption 不存在"));
        red.setStatus(CouponRedemption.RedemptionStatus.REVERSED);
        red.setNote(note);
        red = redemptionRepository.save(red);
        return CouponDtos.RedemptionResponse.builder()
                .redemptionId(red.getId())
                .couponId(red.getCouponId())
                .userId(red.getUserId())
                .orderId(red.getOrderId())
                .amountDiscounted(red.getAmountDiscounted())
                .status(red.getStatus().name())
                .note(red.getNote())
                .build();
    }

    public Page<CouponDtos.RedemptionResponse> listRedemptionsByUser(Long userId, Pageable pageable) {
        return redemptionRepository.findAllByUserIdOrderByRedeemedAtDesc(userId, pageable)
                .map(r -> CouponDtos.RedemptionResponse.builder()
                        .redemptionId(r.getId())
                        .couponId(r.getCouponId())
                        .userId(r.getUserId())
                        .orderId(r.getOrderId())
                        .amountDiscounted(r.getAmountDiscounted())
                        .status(r.getStatus().name())
                        .note(r.getNote())
                        .build());
    }

    private CouponDtos.ValidateResponse invalid(String reason, Coupon c, String code) {
        return CouponDtos.ValidateResponse.builder().valid(false).reason(reason).couponId(c.getId()).code(code).build();
    }

    private int calculateDiscount(Coupon c, int orderAmount) {
        int discount;
        switch (c.getDiscountType()) {
            case PERCENT -> {
                discount = (int) Math.floor(orderAmount * (c.getDiscountValue() / 100.0));
            }
            case FIXED -> {
                discount = c.getDiscountValue();
            }
            default -> discount = 0;
        }
        if (c.getMaxDiscountAmount() != null) {
            discount = Math.min(discount, c.getMaxDiscountAmount());
        }
        discount = Math.min(discount, orderAmount);
        return Math.max(discount, 0);
    }

    private CouponDtos.CouponResponse toResponse(Coupon c) {
        return CouponDtos.CouponResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .codeType(c.getCodeType())
                .genericCode(c.getGenericCode())
                .discountType(c.getDiscountType())
                .discountValue(c.getDiscountValue())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .minSpendAmount(c.getMinSpendAmount())
                .startsAt(c.getStartsAt())
                .endsAt(c.getEndsAt())
                .status(c.getStatus())
                .totalUsageLimit(c.getTotalUsageLimit())
                .perUserLimit(c.getPerUserLimit())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
