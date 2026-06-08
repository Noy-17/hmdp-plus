package org.javaup.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.RecommendResult;
import org.javaup.dto.ShopSearchResult;
import org.javaup.feign.FollowInternalFeignClient;
import org.javaup.feign.SearchFeignClient;
import org.javaup.llm.LlmClient;
import org.javaup.llm.PromptTemplates;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiShopRecommendService {

    private final LlmClient llmClient;
    private final FollowInternalFeignClient followInternalFeignClient;
    private final SearchFeignClient searchFeignClient;
    private final UserProfileService userProfileService;

    public List<RecommendResult> recommend(Long userId) {
        String profileSummary = userProfileService.formatForPrompt(userId);

        StringBuilder socialCtx = new StringBuilder();
        try {
            var followingsResult = followInternalFeignClient.getFollowings(userId);
            List<Long> followings = followingsResult.getData();
            if (followings != null && !followings.isEmpty()) {
                socialCtx.append("关注了").append(followings.size()).append("位用户");
            }
        } catch (Exception e) {
            log.debug("No followings for user {}", userId);
        }

        List<ShopSearchResult> candidates = fetchCandidates(userId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        String shopsJson = JSONUtil.toJsonStr(candidates);
        String prompt = PromptTemplates.SHOP_RECOMMEND + "\n\n用户偏好: " + profileSummary
                + "\n社交关系: " + socialCtx
                + "\n\n候选商铺(JSON): " + shopsJson
                + "\n\n请从中选出最匹配的5个商铺，严格遵守以下字段名:"
                + "\n{\"id\": 商铺ID(数字), \"name\": 商铺名称(字符串), \"reason\": 推荐理由(字符串), \"score\": 匹配度(数字0-1)}"
                + "\n只返回JSON数组，不要其他文字。";

        try {
            String response = llmClient.chat("你是一个商铺推荐引擎。请严格从候选列表中选取，仅返回JSON。", prompt);
            log.info("Shop LLM response: {}", response);
            return parseRecommendResults(response);
        } catch (Exception e) {
            log.error("Shop recommendation failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ShopSearchResult> fetchCandidates(Long userId) {
        Set<Long> seen = new HashSet<>();
        List<ShopSearchResult> candidates = new ArrayList<>();

        // 1. 从用户偏好类型中查店铺
        List<Long> preferredTypeIds = userProfileService.getPreferredTypeIds(userId);
        for (Long typeId : preferredTypeIds) {
            if (candidates.size() >= 20) break;
            try {
                var result = searchFeignClient.searchByType(typeId.intValue(), 1, null, null);
                if (result.getData() != null) {
                    for (ShopSearchResult s : result.getData()) {
                        if (seen.add(s.getId())) {
                            candidates.add(s);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to search type {}", typeId);
            }
        }

        // 2. 不够则用热门类型补充
        for (int defaultType : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}) {
            if (candidates.size() >= 20) break;
            try {
                var result = searchFeignClient.searchByType(defaultType, 1, null, null);
                if (result.getData() != null) {
                    for (ShopSearchResult s : result.getData()) {
                        if (seen.add(s.getId())) {
                            candidates.add(s);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed fallback search type {}", defaultType);
            }
        }

        return candidates;
    }

    private List<RecommendResult> parseRecommendResults(String response) {
        String json = response.trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        JSONArray arr = JSONUtil.parseArray(json);
        List<RecommendResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(arr.size(), 5); i++) {
            results.add(arr.get(i, RecommendResult.class));
        }
        return results;
    }
}
