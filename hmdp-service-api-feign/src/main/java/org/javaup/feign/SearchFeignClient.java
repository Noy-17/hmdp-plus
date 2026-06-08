package org.javaup.feign;

import org.javaup.dto.Result;
import org.javaup.dto.ShopSearchResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "hmdp-search-service", path = "/shop")
public interface SearchFeignClient {

    @GetMapping("/of/name")
    Result<List<ShopSearchResult>> searchByName(
            @RequestParam("name") String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current);

    @GetMapping("/of/type")
    Result<List<ShopSearchResult>> searchByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y);
}
