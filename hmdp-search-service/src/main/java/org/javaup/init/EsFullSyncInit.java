package org.javaup.init;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.service.ShopSearchService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(2)
@Component
public class EsFullSyncInit {

    @Resource
    private ElasticsearchClient esClient;

    @Resource
    private ShopSearchService shopSearchService;

    @PostConstruct
    public void init() {
        log.info("========== ES 全量同步检查 ==========");
        try {
            // 确保索引存在且有正确的 geo_point 映射
            boolean exists = esClient.indices().exists(e -> e.index("shop")).value();
            if (exists) {
                // 删除旧索引（location 可能被错误映射为 object 而非 geo_point）
                esClient.indices().delete(d -> d.index("shop"));
                log.info("已删除旧索引 shop（重建以启用 geo_point 映射）");
            }
            esClient.indices().create(c -> c
                    .index("shop")
                    .mappings(m -> m
                            .properties("location", p -> p.geoPoint(g -> g))
                    ));
            log.info("ES 索引 shop 已创建");
            long count = esClient.count(c -> c.index("shop")).count();
            if (count > 0) {
                log.info("ES 索引已有 {} 条数据，跳过全量同步", count);
                return;
            }
            log.info("ES 索引为空，开始从数据库全量同步...");
            shopSearchService.fullSync();
        } catch (Exception e) {
            log.warn("ES 初始化异常: {}", e.getMessage());
            try {
                shopSearchService.fullSync();
            } catch (Exception ex) {
                log.error("全量同步失败", ex);
            }
        }
    }
}
