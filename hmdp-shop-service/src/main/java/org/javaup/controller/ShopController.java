package org.javaup.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.javaup.dto.Result;
import org.javaup.dto.UserBehaviorEvent;
import org.javaup.entity.Shop;
import org.javaup.rabbitmq.message.ShopSyncMessage;
import org.javaup.rabbitmq.producer.ShopSyncProducer;
import org.javaup.rabbitmq.producer.UserBehaviorProducer;
import org.javaup.service.IShopService;
import org.javaup.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    @Resource
    private ShopSyncProducer shopSyncProducer;
    @Resource
    private UserBehaviorProducer userBehaviorProducer;

    @Value("${prefix.distinction.name:hmdp}")
    private String prefix;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        Result result = shopService.queryById(id);
        emitViewShopEvent(id);
        return result;
    }

    private void emitViewShopEvent(Long shopId) {
        try {
            Long userId = org.javaup.utils.UserHolder.getUser().getId();
            UserBehaviorEvent event = new UserBehaviorEvent();
            event.setUserId(userId);
            event.setEventType("VIEW_SHOP");
            event.setTargetId(shopId);
            event.setTargetType("SHOP");
            event.setTimestamp(System.currentTimeMillis());
            userBehaviorProducer.send(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        Result result = shopService.saveShop(shop);
        if (result.getSuccess() && result.getData() instanceof Long shopId) {
            shopSyncProducer.sendPayload(prefix + "-shop_sync_topic", new ShopSyncMessage(shopId, "CREATE"));
        }
        return result;
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        Result result = shopService.update(shop);
        Long id = shop.getId();
        if (id != null) {
            shopSyncProducer.sendPayload(prefix + "-shop_sync_topic", new ShopSyncMessage(id, "UPDATE"));
        }
        return result;
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
       return shopService.queryShopByType(typeId, current, x, y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
