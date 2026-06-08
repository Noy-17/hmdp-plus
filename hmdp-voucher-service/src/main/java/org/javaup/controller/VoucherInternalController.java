package org.javaup.controller;

import jakarta.annotation.Resource;
import org.javaup.dto.Result;
import org.javaup.dto.VoucherAvailableDto;
import org.javaup.dto.VoucherHistoryDto;
import org.javaup.entity.Voucher;
import org.javaup.entity.VoucherOrder;
import org.javaup.entity.SeckillVoucher;
import org.javaup.mapper.SeckillVoucherMapper;
import org.javaup.mapper.VoucherMapper;
import org.javaup.mapper.VoucherOrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/voucher-internal")
public class VoucherInternalController {

    @Resource
    private VoucherOrderMapper voucherOrderMapper;
    @Resource
    private VoucherMapper voucherMapper;
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @GetMapping("/history/{userId}")
    public Result<List<VoucherHistoryDto>> getPurchaseHistory(@PathVariable Long userId) {
        List<VoucherOrder> orders = voucherOrderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VoucherOrder>()
                        .eq("user_id", userId)
                        .eq("status", 1));

        if (orders.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        List<Long> voucherIds = orders.stream()
                .map(VoucherOrder::getVoucherId)
                .distinct()
                .collect(Collectors.toList());
        List<Voucher> vouchers = voucherMapper.selectBatchIds(voucherIds);
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));

        List<VoucherHistoryDto> result = orders.stream().map(o -> {
            VoucherHistoryDto dto = new VoucherHistoryDto();
            dto.setUserId(o.getUserId());
            dto.setVoucherId(o.getVoucherId());
            dto.setOrderId(o.getId());
            Voucher v = voucherMap.get(o.getVoucherId());
            if (v != null) {
                dto.setShopId(v.getShopId());
                dto.setVoucherTitle(v.getTitle());
            }
            dto.setPurchaseTime(o.getCreateTime());
            return dto;
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    @GetMapping("/available")
    public Result<List<VoucherAvailableDto>> getAvailableVouchers() {
        List<Voucher> vouchers = voucherMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Voucher>()
                        .eq("status", 1));

        if (vouchers.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        List<Long> voucherIds = vouchers.stream()
                .map(Voucher::getId)
                .collect(Collectors.toList());
        List<SeckillVoucher> seckillVouchers = seckillVoucherMapper.selectBatchIds(voucherIds);
        Map<Long, SeckillVoucher> seckillMap = seckillVouchers.stream()
                .collect(Collectors.toMap(SeckillVoucher::getVoucherId, sv -> sv));

        List<VoucherAvailableDto> result = vouchers.stream().map(v -> {
            VoucherAvailableDto dto = new VoucherAvailableDto();
            dto.setId(v.getId());
            dto.setShopId(v.getShopId());
            dto.setTitle(v.getTitle());
            dto.setSubTitle(v.getSubTitle());
            dto.setPayValue(v.getPayValue());
            dto.setActualValue(v.getActualValue());
            dto.setType(v.getType());
            SeckillVoucher sv = seckillMap.get(v.getId());
            if (sv != null) {
                dto.setStock(sv.getStock());
                dto.setBeginTime(sv.getBeginTime());
                dto.setEndTime(sv.getEndTime());
            }
            return dto;
        }).collect(Collectors.toList());

        return Result.ok(result);
    }
}
