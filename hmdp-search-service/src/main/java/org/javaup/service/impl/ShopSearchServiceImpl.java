package org.javaup.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.document.ShopDoc;
import org.javaup.entity.Shop;
import org.javaup.mapper.ShopMapper;
import org.javaup.service.ShopSearchService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ShopSearchServiceImpl implements ShopSearchService {

    private static final String INDEX = "shop";

    @Resource
    private ElasticsearchClient esClient;

    @Resource
    private ShopMapper shopMapper;

    @Override
    public List<ShopDoc> searchByName(String keyword, int page, int size) {
        try {
            SearchResponse<ShopDoc> response = esClient.search(s -> s
                    .index(INDEX)
                    .query(q -> q.match(m -> m.field("name").query(keyword)))
                    .from((page - 1) * size)
                    .size(size)
                    .sort(sort -> sort.field(f -> f.field("score").order(SortOrder.Desc))),
                    ShopDoc.class);
            return response.hits().hits().stream().map(Hit::source).toList();
        } catch (Exception e) {
            log.error("ES searchByName error", e);
            return List.of();
        }
    }

    @Override
    public List<ShopDoc> searchByTypeAndLocation(Long typeId, Double x, Double y, int page, int size) {
        try {
            SearchResponse<ShopDoc> response;
            if (x != null && y != null) {
                response = esClient.search(s -> s
                        .index(INDEX)
                        .query(q -> q.bool(b -> b
                                .must(m -> m.term(t -> t.field("typeId").value(FieldValue.of(typeId))))
                                .filter(f -> f.geoDistance(g -> g
                                        .field("location")
                                        .location(l -> l.latlon(ll -> ll.lat(y).lon(x)))
                                        .distance("5000m")))))
                        .sort(sort -> sort.geoDistance(g -> g
                                .field("location")
                                .location(l -> l.latlon(ll -> ll.lat(y).lon(x)))
                                .order(SortOrder.Asc)))
                        .from((page - 1) * size)
                        .size(size),
                        ShopDoc.class);
            } else {
                response = esClient.search(s -> s
                        .index(INDEX)
                        .query(q -> q.term(t -> t.field("typeId").value(FieldValue.of(typeId))))
                        .sort(sort -> sort.field(f -> f.field("score").order(SortOrder.Desc)))
                        .from((page - 1) * size)
                        .size(size),
                        ShopDoc.class);
            }
            List<ShopDoc> docs = new ArrayList<>();
            for (Hit<ShopDoc> hit : response.hits().hits()) {
                ShopDoc doc = hit.source();
                if (doc != null && hit.sort() != null && !hit.sort().isEmpty()) {
                    FieldValue sv = hit.sort().get(0);
                    if (sv.isDouble()) {
                        doc.setDistance(sv.doubleValue());
                    } else if (sv.isLong()) {
                        doc.setDistance((double) sv.longValue());
                    }
                }
                if (doc != null) {
                    docs.add(doc);
                }
            }
            return docs;
        } catch (Exception e) {
            log.error("ES searchByTypeAndLocation error", e);
            return List.of();
        }
    }

    @Override
    public void syncFromShop(Shop shop) {
        try {
            ShopDoc doc = toDoc(shop);
            esClient.index(i -> i.index(INDEX).id(String.valueOf(doc.getId())).document(doc));
            log.debug("ES synced: shop id={}", shop.getId());
        } catch (Exception e) {
            log.error("ES sync error: shop id={}", shop.getId(), e);
        }
    }

    @Override
    public void deleteFromEs(Long shopId) {
        try {
            esClient.delete(d -> d.index(INDEX).id(String.valueOf(shopId)));
            log.debug("ES deleted: shop id={}", shopId);
        } catch (Exception e) {
            log.error("ES delete error: shop id={}", shopId, e);
        }
    }

    @Override
    public void fullSync() {
        List<Shop> shops = shopMapper.selectList(null);
        if (shops.isEmpty()) {
            log.info("No shops in DB, skipping full sync");
            return;
        }
        int batchSize = 500;
        for (int i = 0; i < shops.size(); i += batchSize) {
            int end = Math.min(i + batchSize, shops.size());
            List<Shop> batch = shops.subList(i, end);
            BulkRequest.Builder bulk = new BulkRequest.Builder();
            for (Shop shop : batch) {
                ShopDoc doc = toDoc(shop);
                bulk.operations(op -> op.index(idx -> idx
                        .index(INDEX)
                        .id(String.valueOf(doc.getId()))
                        .document(doc)));
            }
            try {
                esClient.bulk(bulk.build());
            } catch (Exception e) {
                log.error("ES bulk index error at batch {}/{}", i / batchSize, shops.size() / batchSize, e);
            }
        }
        log.info("Full sync completed: {} shops indexed", shops.size());
    }

    private ShopDoc toDoc(Shop shop) {
        ShopDoc doc = new ShopDoc();
        doc.setId(shop.getId());
        doc.setName(shop.getName());
        doc.setAddress(shop.getAddress());
        doc.setArea(shop.getArea());
        doc.setTypeId(shop.getTypeId());
        doc.setImages(shop.getImages());
        doc.setAvgPrice(shop.getAvgPrice());
        doc.setSold(shop.getSold());
        doc.setComments(shop.getComments());
        doc.setScore(shop.getScore());
        doc.setOpenHours(shop.getOpenHours());
        doc.setCreateTime(shop.getCreateTime());
        doc.setUpdateTime(shop.getUpdateTime());
        if (shop.getX() != null && shop.getY() != null) {
            doc.setLocation(new ShopDoc.Location(shop.getY(), shop.getX()));
        }
        return doc;
    }
}
