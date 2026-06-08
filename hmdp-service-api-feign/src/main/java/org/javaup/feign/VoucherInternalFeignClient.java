package org.javaup.feign;

import org.javaup.dto.Result;
import org.javaup.dto.VoucherAvailableDto;
import org.javaup.dto.VoucherHistoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "hmdp-voucher-service", path = "/voucher-internal")
public interface VoucherInternalFeignClient {

    @GetMapping("/history/{userId}")
    Result<List<VoucherHistoryDto>> getPurchaseHistory(@PathVariable("userId") Long userId);

    @GetMapping("/available")
    Result<List<VoucherAvailableDto>> getAvailableVouchers();
}
