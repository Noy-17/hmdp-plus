package org.javaup.service;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.AiSearchRequest;
import org.javaup.dto.AiSearchResponse;
import org.javaup.dto.ShopSearchResult;
import org.javaup.dto.ShopTypeDto;
import org.javaup.feign.SearchFeignClient;
import org.javaup.feign.ShopTypeFeignClient;
import org.javaup.llm.LlmClient;
import org.javaup.llm.LlmToolResponse;
import org.javaup.llm.PromptTemplates;
import org.javaup.llm.SearchIntent;
import org.javaup.llm.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchService {

    private final LlmClient llmClient;
    private final SearchFeignClient searchFeignClient;
    private final ShopTypeFeignClient shopTypeFeignClient;

    public AiSearchResponse search(AiSearchRequest request) {
        ToolDefinition searchTool = buildSearchTool();

        LlmToolResponse toolResult = llmClient.chatWithTools(
                PromptTemplates.SEARCH_EXTRACTION,
                request.getQuery(),
                List.of(searchTool));

        if (!toolResult.isToolCalled()) {
            return buildOutOfScope();
        }

        SearchIntent intent = parseIntent(toolResult.getArguments());
        List<ShopSearchResult> shops = executeSearch(intent);
        return new AiSearchResponse(shops, buildIntentMessage(intent, shops.size()));
    }

    private ToolDefinition buildSearchTool() {
        String typeMapping = fetchTypeMapping();
        return new ToolDefinition(
                "extractSearchParams",
                "从自然语言中提取商铺搜索参数。与商铺搜索无关的查询不要调用此工具。"
                        + (typeMapping.isEmpty() ? "" : "\n\n可用的商铺类型及ID:\n" + typeMapping),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "商铺名称关键字，仅用户明确说到商铺名时才填写。不要将菜系/食物/娱乐类型名（如火锅、川菜、日料）填入此字段"),
                                "typeId", Map.of("type", "integer", "description", "商铺类型ID，从可用类型列表中选最匹配的。食物类(火锅/川菜/日料/烤鸭/茶饮等)→1(美食)，KTV→2，美发→3，美甲→10，按摩/足疗→5，SPA/美容→6，亲子→7，酒吧→8，轰趴→9，健身/运动→4"),
                                "area", Map.of("type", "string", "description", "区域名如西湖区、拱墅区"),
                                "avgPriceMin", Map.of("type", "number", "description", "最低人均价格"),
                                "avgPriceMax", Map.of("type", "number", "description", "最高人均价格"),
                                "sortBy", Map.of("type", "string", "enum", List.of("score", "avgPrice", "distance"), "description", "排序字段"),
                                "sortOrder", Map.of("type", "string", "enum", List.of("asc", "desc"), "description", "排序方向")
                        )
                )
        );
    }

    private String fetchTypeMapping() {
        try {
            List<ShopTypeDto> types = shopTypeFeignClient.getTypeList().getData();
            if (types == null || types.isEmpty()) return "";
            return types.stream()
                    .map(t -> t.getId() + ":" + t.getName())
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.debug("Failed to fetch shop types: {}", e.getMessage());
            return "";
        }
    }

    private SearchIntent parseIntent(Map<String, Object> args) {
        return JSONUtil.toBean(JSONUtil.toJsonStr(args), SearchIntent.class);
    }

    private List<ShopSearchResult> executeSearch(SearchIntent intent) {
        try {
            // Prefer typeId if available, then keyword
            if (intent.getTypeId() != null) {
                var result = searchFeignClient.searchByType(intent.getTypeId(), 1, null, null);
                return postFilter(result.getData(), intent);
            }
            if (intent.getKeyword() != null) {
                var result = searchFeignClient.searchByName(intent.getKeyword(), 1);
                return postFilter(result.getData(), intent);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Search execution failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ShopSearchResult> postFilter(List<ShopSearchResult> shops, SearchIntent intent) {
        if (shops == null) {
            return Collections.emptyList();
        }
        return shops.stream()
                .filter(s -> intent.getArea() == null || s.getArea() != null && s.getArea().contains(intent.getArea()))
                .filter(s -> intent.getAvgPriceMin() == null || s.getAvgPrice() >= intent.getAvgPriceMin())
                .filter(s -> intent.getAvgPriceMax() == null || s.getAvgPrice() <= intent.getAvgPriceMax())
                .limit(20)
                .toList();
    }

    private String buildIntentMessage(SearchIntent intent, int resultCount) {
        StringBuilder sb = new StringBuilder();
        if (intent.getKeyword() != null) {
            sb.append("搜索").append(intent.getKeyword());
        }
        if (intent.getArea() != null) {
            if (!sb.isEmpty()) sb.append("，");
            sb.append("区域").append(intent.getArea());
        }
        if (intent.getAvgPriceMin() != null && intent.getAvgPriceMax() != null) {
            if (!sb.isEmpty()) sb.append("，");
            sb.append("人均").append(intent.getAvgPriceMin().intValue()).append("-").append(intent.getAvgPriceMax().intValue()).append("元");
        } else if (intent.getAvgPriceMax() != null) {
            if (!sb.isEmpty()) sb.append("，");
            sb.append("人均").append(intent.getAvgPriceMax().intValue()).append("元以内");
        } else if (intent.getAvgPriceMin() != null) {
            if (!sb.isEmpty()) sb.append("，");
            sb.append("人均").append(intent.getAvgPriceMin().intValue()).append("元以上");
        }
        if (sb.isEmpty()) {
            return resultCount == 0 ? "未找到匹配的商铺" : "为您找到" + resultCount + "家商铺";
        }
        if (resultCount == 0) {
            sb.append("，未找到匹配的商铺");
        } else {
            sb.append("，为您找到").append(resultCount).append("家商铺");
        }
        return sb.toString();
    }

    private AiSearchResponse buildOutOfScope() {
        return new AiSearchResponse(
                Collections.emptyList(),
                "抱歉，我只能帮您搜索商铺。请描述您想找的商铺类型、区域或价位，例如：'西湖区评分高的火锅店，人均100以内'");
    }
}
