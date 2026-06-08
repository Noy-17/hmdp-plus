package org.javaup.feign;

import org.javaup.dto.Result;
import org.javaup.dto.ShopTypeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "hmdp-shop-service", path = "/shop-type")
public interface ShopTypeFeignClient {

    @GetMapping("list")
    Result<List<ShopTypeDto>> getTypeList();
}
