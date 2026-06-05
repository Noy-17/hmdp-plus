package org.javaup.controller;


import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.javaup.dto.DelayVoucherReminderDto;
import org.javaup.dto.GetSeckillVoucherDto;
import org.javaup.dto.Result;
import org.javaup.dto.SeckillVoucherDto;
import org.javaup.dto.UpdateSeckillVoucherDto;
import org.javaup.dto.UpdateSeckillVoucherStockDto;
import org.javaup.dto.VoucherDto;
import org.javaup.dto.VoucherSubscribeBatchDto;
import org.javaup.dto.VoucherSubscribeDto;
import org.javaup.entity.Voucher;
import org.javaup.model.SeckillVoucherFullModel;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IVoucherService;
import org.javaup.vo.GetSubscribeStatusVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;
    
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    
    @PostMapping("/get")
    public Result<SeckillVoucherFullModel> get(@Valid @RequestBody GetSeckillVoucherDto getSeckillVoucherDto) {
        return Result.ok(seckillVoucherService.queryByVoucherId(getSeckillVoucherDto.getVoucherId()));
    }
    
    @PostMapping("/seckill")
    public Result<Long> addSeckillVoucher(@Valid @RequestBody SeckillVoucherDto seckillVoucherDto) {
        final Long voucherId = voucherService.addSeckillVoucher(seckillVoucherDto);
        return Result.ok(voucherId);
    }

    @PostMapping("/update/seckill")
    public Result<Void> updateSeckillVoucher(@Valid @RequestBody UpdateSeckillVoucherDto updateSeckillVoucherDto) {
        voucherService.updateSeckillVoucher(updateSeckillVoucherDto);
        return Result.ok();
    }
    
    @PostMapping("/update/seckill/stock")
    public Result<Void> updateSeckillVoucherStock(@Valid @RequestBody UpdateSeckillVoucherStockDto updateSeckillVoucherDto) {
        voucherService.updateSeckillVoucherStock(updateSeckillVoucherDto);
        return Result.ok();
    }

    @PostMapping
    public Result<Long> addVoucher(@Valid @RequestBody VoucherDto voucherDto) {
        final Long voucherId = voucherService.addVoucher(voucherDto);
        return Result.ok(voucherId);
    }
    
    @GetMapping("/list/{shopId}")
    public Result<List<Voucher>> queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
    
    @PostMapping("/subscribe")
    public Result<Void> subscribe(@Valid @RequestBody VoucherSubscribeDto voucherSubscribeDto){
        voucherService.subscribe(voucherSubscribeDto);
        return Result.ok();
    }
    
    @PostMapping("/unsubscribe")
    public Result<Void> unsubscribe(@Valid @RequestBody VoucherSubscribeDto voucherSubscribeDto){
        voucherService.unsubscribe(voucherSubscribeDto);
        return Result.ok();
    }
    
    @PostMapping("/get/subscribe/status")
    public Result<Integer> getSubscribeStatus(@Valid @RequestBody VoucherSubscribeDto voucherSubscribeDto){
        return Result.ok(voucherService.getSubscribeStatus(voucherSubscribeDto));
    }
    
    @PostMapping("/get/subscribe/status/batch")
    public Result<List<GetSubscribeStatusVo>> getSubscribeStatusBatch(@Valid @RequestBody VoucherSubscribeBatchDto voucherSubscribeBatchDto){
        return Result.ok(voucherService.getSubscribeStatusBatch(voucherSubscribeBatchDto));
    }
    
    @PostMapping("/delay/voucher/reminder")
    public Result<Void> delayVoucherReminder(@Valid @RequestBody DelayVoucherReminderDto delayVoucherReminderDto){
        voucherService.delayVoucherReminder(delayVoucherReminderDto);
        return Result.ok();
    }
}
