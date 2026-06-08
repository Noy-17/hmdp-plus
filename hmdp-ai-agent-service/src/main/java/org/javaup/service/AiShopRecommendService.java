package org.javaup.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.RecommendResult;
import org.javaup.feign.FollowInternalFeignClient;
import org.javaup.llm.LlmClient;
import org.javaup.llm.PromptTemplates;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiShopRecommendService {

    private final LlmClient llmClient;
    private final FollowInternalFeignClient followInternalFeignClient;
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

        String prompt = PromptTemplates.SHOP_RECOMMEND + "\n\n用户偏好: " + profileSummary
                + "\n社交关系: " + socialCtx
                + "\n\n请基于用户偏好推荐5个商铺类型和价位建议，严格遵守以下字段名:"
                + "\n{\"name\": 类型名称(字符串), \"reason\": 推荐理由(字符串), \"score\": 匹配度(数字0-1)}"
                + "\n只返回JSON数组，不要其他文字。";

        try {
            String response = llmClient.chat("你是一个商铺推荐引擎。请仅返回JSON，不要有任何其他内容。", prompt);
            log.info("Shop LLM response: {}", response);
            return parseRecommendResults(response);
        } catch (Exception e) {
            log.error("Shop recommendation failed: {}", e.getMessage());
            return List.of();
        }
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
