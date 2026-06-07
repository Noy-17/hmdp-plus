package org.javaup.controller;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.javaup.document.ShopDoc;
import org.javaup.dto.Result;
import org.javaup.service.ShopSearchService;
import org.javaup.utils.SystemConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shop")
public class ShopSearchController {

    @Resource
    private ShopSearchService shopSearchService;

    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        String keyword = StrUtil.isNotBlank(name) ? name : "";
        List<ShopDoc> docs = shopSearchService.searchByName(keyword, current, SystemConstants.MAX_PAGE_SIZE);
        return Result.ok(docs);
    }

    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y) {
        List<ShopDoc> docs = shopSearchService.searchByTypeAndLocation(
                typeId.longValue(), x, y,
                current, SystemConstants.DEFAULT_PAGE_SIZE);
        return Result.ok(docs);
    }
}
