package org.javaup.controller;

import lombok.RequiredArgsConstructor;
import org.javaup.dto.AiSearchRequest;
import org.javaup.dto.AiSearchResponse;
import org.javaup.dto.RecommendResult;
import org.javaup.dto.Result;
import org.javaup.service.AiSearchService;
import org.javaup.service.AiShopRecommendService;
import org.javaup.service.AiVoucherRecommendService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiSearchService aiSearchService;
    private final AiVoucherRecommendService aiVoucherRecommendService;
    private final AiShopRecommendService aiShopRecommendService;

    @PostMapping("/search")
    public Result<AiSearchResponse> search(@RequestBody AiSearchRequest request) {
        return Result.ok(aiSearchService.search(request));
    }

    @PostMapping("/recommend/voucher")
    public Result<List<RecommendResult>> recommendVoucher(@RequestBody org.javaup.dto.AiVoucherRecommendRequest request) {
        return Result.ok(aiVoucherRecommendService.recommend(request.getUserId()));
    }

    @PostMapping("/recommend/shop")
    public Result<List<RecommendResult>> recommendShop(@RequestBody org.javaup.dto.AiShopRecommendRequest request) {
        return Result.ok(aiShopRecommendService.recommend(request.getUserId()));
    }
}
