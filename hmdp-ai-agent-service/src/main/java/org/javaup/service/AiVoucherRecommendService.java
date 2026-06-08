package org.javaup.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.RecommendResult;
import org.javaup.dto.UserInfoDTO;
import org.javaup.dto.VoucherAvailableDto;
import org.javaup.dto.VoucherHistoryDto;
import org.javaup.feign.UserInfoFeignClient;
import org.javaup.feign.VoucherInternalFeignClient;
import org.javaup.llm.LlmClient;
import org.javaup.llm.PromptTemplates;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVoucherRecommendService {

    private final LlmClient llmClient;
    private final VoucherInternalFeignClient voucherInternalFeignClient;
    private final UserInfoFeignClient userInfoFeignClient;
    private final UserProfileService userProfileService;

    public List<RecommendResult> recommend(Long userId) {
        String profileSummary = userProfileService.formatForPrompt(userId);

        // cold start fallback: use purchase history directly
        if (profileSummary.isEmpty()) {
            profileSummary = buildColdStartProfile(userId);
        }

        List<VoucherAvailableDto> availableList;
        try {
            availableList = voucherInternalFeignClient.getAvailableVouchers().getData();
        } catch (Exception e) {
            log.warn("Failed to fetch available vouchers: {}", e.getMessage());
            return List.of();
        }
        if (availableList == null || availableList.isEmpty()) {
            return List.of();
        }

        String voucherListJson = JSONUtil.toJsonStr(availableList);
        String prompt = PromptTemplates.VOUCHER_RECOMMEND + "\n\n用户偏好: " + profileSummary
                + "\n\n当前可用优惠券(JSON): " + voucherListJson
                + "\n\n请以JSON数组格式返回推荐结果，严格遵守以下字段名:"
                + "\n{\"id\": 优惠券ID(数字), \"name\": 标题(字符串), \"reason\": 推荐理由(字符串), \"score\": 匹配度(数字0-1)}"
                + "\n只返回JSON数组，不要其他文字。";

        try {
            String response = llmClient.chat("你是一个个性化推荐引擎。请仅返回JSON，不要有任何其他内容。", prompt);
            log.info("Voucher LLM response: {}", response);
            return parseRecommendResults(response);
        } catch (Exception e) {
            log.error("Voucher recommendation failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildColdStartProfile(Long userId) {
        StringBuilder sb = new StringBuilder();
        try {
            var historyResult = voucherInternalFeignClient.getPurchaseHistory(userId);
            List<VoucherHistoryDto> history = historyResult.getData();
            if (history != null && !history.isEmpty()) {
                sb.append("已购买").append(history.size()).append("张优惠券:");
                history.forEach(h -> sb.append(h.getVoucherTitle()).append(","));
            }
        } catch (Exception e) {
            log.debug("No purchase history for user {}", userId);
        }
        try {
            var infoResult = userInfoFeignClient.getInfoByUserId(userId);
            UserInfoDTO info = infoResult.getData();
            if (info != null) {
                sb.append(" 用户等级:").append(info.getLevel());
            }
        } catch (Exception e) {
            log.debug("No user info for user {}", userId);
        }
        return sb.toString();
    }

    private List<RecommendResult> parseRecommendResults(String response) {
        String json = response.trim();
        // extract JSON array from response (LLM may add markdown code blocks)
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
