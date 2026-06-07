package org.javaup.service;

import org.javaup.document.ShopDoc;
import org.javaup.entity.Shop;

import java.util.List;

public interface ShopSearchService {

    List<ShopDoc> searchByName(String keyword, int page, int size);

    List<ShopDoc> searchByTypeAndLocation(Long typeId, Double x, Double y, int page, int size);

    void syncFromShop(Shop shop);

    void deleteFromEs(Long shopId);

    void fullSync();
}
