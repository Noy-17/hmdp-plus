package org.javaup.delay.consumer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.ConsumerTask;
import org.javaup.core.RedisKeyManage;
import org.javaup.core.SpringUtil;
import org.javaup.delay.message.DelayedVoucherReminderMessage;
import org.javaup.dto.LevelQueryRequest;
import org.javaup.dto.UserInfoDTO;
import org.javaup.feign.UserInfoFeignClient;
import org.javaup.model.SeckillVoucherFullModel;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.ISeckillVoucherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.javaup.constant.Constant.DELAY_VOUCHER_REMINDER;


@Slf4j
@Component
public class ConsumerDelayedVoucherReminder implements ConsumerTask {
    
    @Resource
    private RedisCache redisCache;
    
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    
    @Resource
    private UserInfoFeignClient userInfoFeignClient;


    @Value("${seckill.reminder.notify.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${seckill.reminder.notify.app.enabled:false}")
    private boolean appEnabled;

    @Value("${seckill.reminder.notify.sms.to:}")
    private String smsTo;

    @Value("${seckill.reminder.dedup.window.seconds:1800}")
    private long dedupWindowSeconds;

    /**
     * 当优惠券未设置allowedLevels/minLevel时的默认最小会员等级
     * */
    @Value("${seckill.reminder.notify.default.minLevel:1}")
    private int defaultMinLevel;
    /**
     * 每次提醒的最大用户数量，防止一次性通知过多
     * */
    @Value("${seckill.reminder.notify.max.users:1000}")
    private int maxNotifyUsers;
    /**
     * 是否附加通知“最近购买活跃用户”
     * */
    @Value("${seckill.reminder.notify.top.buyers.enabled:true}")
    private boolean topBuyersEnabled;
    /**
     * 统计最近多少天的购买行为
     * */
    @Value("${seckill.reminder.notify.top.buyers.days:30}")
    private int topBuyersDays;
    /**
     * Top购买用户数量
     * */
    @Value("${seckill.reminder.notify.top.buyers.count:200}")
    private int topBuyersCount;
    /**
     * 最大会员等级
     */
    @Value("${seckill.reminder.notify.user.level.max:10}")
    private int maxUserLevel;
    
    @Override
    public void execute(final String content) {
        try {
            DelayedVoucherReminderMessage msg = parseMessage(content);
            if (Objects.isNull(msg)) { 
                return; 
            }
            Long voucherId = msg.getVoucherId();
            SeckillVoucherFullModel voucherFull = seckillVoucherService.queryByVoucherId(voucherId);
            if (voucherFull == null) {
                log.warn("[DELAY_REMINDER_CONSUMER] 秒杀券不存在或缓存未命中 voucherId={}", voucherId);
                return;
            }
            Set<String> userIds = buildAudienceUserIds(voucherFull);
            if (CollectionUtil.isEmpty(userIds)) {
                log.info("[DELAY_REMINDER_CONSUMER] 无符合规则的用户 voucherId={}", voucherId);
                return;
            }
            int notified = notifyUsers(voucherId, msg.getBeginTime(), userIds);
            log.info("[DELAY_REMINDER_CONSUMER] 完成提醒 voucherId={} totalUsers={} notified={}",
                    voucherId, userIds.size(), notified);
        } catch (Exception e) {
            log.warn("[DELAY_REMINDER_CONSUMER] 执行异常", e);
        }
    }

    private DelayedVoucherReminderMessage parseMessage(String content) {
        try {
            DelayedVoucherReminderMessage msg = JSON.parseObject(content, DelayedVoucherReminderMessage.class);
            if (msg == null || msg.getVoucherId() == null) {
                log.warn("[DELAY_REMINDER_CONSUMER] 消息解析失败 content={}", content);
                return null;
            }
            return msg;
        } catch (Exception ex) {
            log.warn("[DELAY_REMINDER_CONSUMER] 消息反序列化异常 content={}", content, ex);
            return null;
        }
    }

    private Set<String> buildAudienceUserIds(SeckillVoucherFullModel voucherFull) {
        String allowedLevelsStr = voucherFull.getAllowedLevels();
        Integer minLevel = voucherFull.getMinLevel();
        Long shopId = voucherFull.getShopId();
        List<UserInfoDTO> userInfos = queryEligibleUserInfos(allowedLevelsStr, minLevel);
        Set<String> userIds = toUserIdSet(userInfos);
        if (topBuyersEnabled && Objects.nonNull(shopId)) {
            for (Long uid : readTopBuyersFromRedis(shopId, topBuyersCount, topBuyersDays)) {
                if (uid != null) { 
                    userIds.add(String.valueOf(uid)); 
                }
            }
        } else if (topBuyersEnabled) {
            log.warn("[DELAY_REMINDER_CONSUMER] 店铺ID为空，跳过Top买家统计");
        }
        return userIds;
    }

    private List<UserInfoDTO> queryEligibleUserInfos(String allowedLevelsStr, Integer minLevel) {
        if (StrUtil.isNotBlank(allowedLevelsStr)) {
            Set<Integer> allowed = parseAllowedLevels(allowedLevelsStr);
            if (CollectionUtil.isNotEmpty(allowed)) {
                List<Long> fromRedis = readUserIdsFromLevelSets(new ArrayList<>(allowed), maxNotifyUsers);
                if (CollectionUtil.isNotEmpty(fromRedis)) {
                    List<UserInfoDTO> list = new ArrayList<>(fromRedis.size());
                    for (Long uid : fromRedis) {
                        if (uid != null) {
                            UserInfoDTO u = new UserInfoDTO();
                            u.setUserId(uid);
                            list.add(u);
                        }
                    }
                    return list;
                }
                LevelQueryRequest req = new LevelQueryRequest();
                req.setLevels(allowed);
                req.setLimit(maxNotifyUsers);
                return userInfoFeignClient.listByLevels(req).getData();
            }
            int useMin = Objects.nonNull(minLevel) ? minLevel : defaultMinLevel;
            List<Long> fromRedis = readUserIdsFromLevelSets(buildLevelRange(useMin, maxUserLevel), maxNotifyUsers);
            if (CollectionUtil.isNotEmpty(fromRedis)) {
                List<UserInfoDTO> list = new ArrayList<>(fromRedis.size());
                for (Long uid : fromRedis) {
                    if (uid != null) {
                        UserInfoDTO u = new UserInfoDTO();
                        u.setUserId(uid);
                        list.add(u);
                    }
                }
                return list;
            }
            LevelQueryRequest req = new LevelQueryRequest();
            req.setMinLevel(useMin);
            req.setLimit(maxNotifyUsers);
            return userInfoFeignClient.listByLevels(req).getData();
        }
        if (Objects.nonNull(minLevel)) {
            List<Long> fromRedis = readUserIdsFromLevelSets(buildLevelRange(minLevel, maxUserLevel), maxNotifyUsers);
            if (CollectionUtil.isNotEmpty(fromRedis)) {
                List<UserInfoDTO> list = new ArrayList<>(fromRedis.size());
                for (Long uid : fromRedis) {
                    if (uid != null) {
                        UserInfoDTO u = new UserInfoDTO();
                        u.setUserId(uid);
                        list.add(u);
                    }
                }
                return list;
            }
            LevelQueryRequest req = new LevelQueryRequest();
            req.setMinLevel(minLevel);
            req.setLimit(maxNotifyUsers);
            return userInfoFeignClient.listByLevels(req).getData();
        }
        List<Long> fromRedis = readUserIdsFromLevelSets(buildLevelRange(defaultMinLevel, maxUserLevel), maxNotifyUsers);
        if (CollectionUtil.isNotEmpty(fromRedis)) {
            List<UserInfoDTO> list = new ArrayList<>(fromRedis.size());
            for (Long uid : fromRedis) {
                if (uid != null) {
                    UserInfoDTO u = new UserInfoDTO();
                    u.setUserId(uid);
                    list.add(u);
                }
            }
            return list;
        }
        LevelQueryRequest req = new LevelQueryRequest();
        req.setMinLevel(defaultMinLevel);
        req.setLimit(maxNotifyUsers);
        return userInfoFeignClient.listByLevels(req).getData();
    }

    private List<Integer> buildLevelRange(int min, int max) {
        int from = Math.max(min, 1);
        int to = Math.max(max, from);
        List<Integer> levels = new ArrayList<>(to - from + 1);
        for (int lv = from; lv <= to; lv++) { 
            levels.add(lv); 
        }
        return levels;
    }

    private List<Long> readUserIdsFromLevelSets(List<Integer> levels, int count) {
        if (CollectionUtil.isEmpty(levels)) { 
            return Collections.emptyList(); 
        }
        List<RedisKeyBuild> keys = new ArrayList<>(levels.size());
        for (Integer lv : levels) {
            if (lv == null) { 
                continue; 
            }
            keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_LEVEL_MEMBERS_TAG_KEY, lv));
        }
        if (keys.isEmpty()) { 
            return Collections.emptyList();
        }
        if (keys.size() == 1) {
            Set<Long> r = redisCache.distinctRandomMembersForSet(keys.get(0), Math.max(count, 1), Long.class);
            return new ArrayList<>(r);
        }
        String label;
        if (levels.size() >= 2) { 
            label = levels.get(0) + "-" + levels.get(levels.size()-1); 
        } else { 
            label = String.valueOf(levels.get(0)); 
        }
        RedisKeyBuild dest = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_LEVEL_MEMBERS_UNION_TAG_KEY, label);
        RedisKeyBuild base = keys.get(0);
        Collection<RedisKeyBuild> others = keys.subList(1, keys.size());
        try {
            redisCache.unionAndStoreForSet(base, others, dest);
            redisCache.expire(dest, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[DELAY_REMINDER_CONSUMER] SET并集失败 levels={} label={}", levels, label, e);
        }
        Set<Long> r = redisCache.distinctRandomMembersForSet(dest, Math.max(count, 1), Long.class);
        return new ArrayList<>(r);
    }

    private Set<Integer> parseAllowedLevels(String allowedLevelsStr) {
        Set<Integer> allowed = new HashSet<>();
        if (StrUtil.isBlank(allowedLevelsStr)) { return allowed; }
        String[] parts = allowedLevelsStr.split(",");
        for (String s : parts) {
            if (StrUtil.isNotBlank(s)) {
                try { 
                    allowed.add(Integer.valueOf(s.trim())); 
                } catch (Exception ignore) {
                    
                }
            }
        }
        return allowed;
    }

    private Set<String> toUserIdSet(List<UserInfoDTO> userInfos) {
        Set<String> userIds = new LinkedHashSet<>();
        if (CollectionUtil.isEmpty(userInfos)) { return userIds; }
        for (UserInfoDTO ui : userInfos) {
            if (Objects.nonNull(ui) && Objects.nonNull(ui.getUserId())) {
                userIds.add(String.valueOf(ui.getUserId()));
            }
        }
        return userIds;
    }

    private List<Long> readTopBuyersFromRedis(Long shopId, int count, int days) {
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.BASIC_ISO_DATE;
            List<RedisKeyBuild> dailyKeys = new ArrayList<>();
            for (int i = 0; i < Math.max(days, 1); i++) {
                String day = today.minusDays(i).format(fmt);
                dailyKeys.add(RedisKeyBuild.createRedisKey(
                        RedisKeyManage.SECKILL_SHOP_TOP_BUYERS_DAILY_TAG_KEY,
                        shopId,
                        day
                ));
            }
            if (dailyKeys.isEmpty()) { 
                return Collections.emptyList(); 
            }
            if (dailyKeys.size() == 1) {
                Set<Long> topSet = redisCache.getReverseRangeForSortedSet(
                        dailyKeys.get(0), 0, Math.max(count - 1, 0), Long.class);
                return new ArrayList<>(topSet);
            }
            String rangeLabel = today.minusDays(dailyKeys.size() - 1).format(fmt) + "-" + today.format(fmt);
            RedisKeyBuild destKey = RedisKeyBuild.createRedisKey(
                    RedisKeyManage.SECKILL_SHOP_TOP_BUYERS_UNION_TAG_KEY,
                    shopId,
                    rangeLabel
            );
            RedisKeyBuild base = dailyKeys.get(0);
            Collection<RedisKeyBuild> others = dailyKeys.subList(1, dailyKeys.size());
            try {
                redisCache.unionAndStoreForSortedSet(base, others, destKey);
                redisCache.expire(destKey, 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("[DELAY_REMINDER_CONSUMER] ZSET并集失败 shopId={} range={}", shopId, rangeLabel, e);
            }
            Set<Long> topSet = redisCache.getReverseRangeForSortedSet(destKey, 0, Math.max(count - 1, 0), Long.class);
            return new ArrayList<>(topSet);
        } catch (Exception ex) {
            log.warn("[DELAY_REMINDER_CONSUMER] 读取Redis Top买家失败 shopId={} days={} count={} ex={}",
                    shopId, days, count, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private int notifyUsers(Long voucherId, LocalDateTime beginTime, Set<String> userIds) {
        int notifyCount = 0;
        for (String userIdStr : userIds) {
            if (StrUtil.isBlank(userIdStr)) { continue; }
            boolean shouldNotify;
            try {
                shouldNotify = redisCache.setIfAbsent(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_REMINDER_NOTIFY_DEDUP_KEY, voucherId, userIdStr),
                        "1",
                        dedupWindowSeconds,
                        java.util.concurrent.TimeUnit.SECONDS
                );
            } catch (Exception e) {
                shouldNotify = true;
            }
            if (!shouldNotify) { 
                continue; 
            }
            String notifyContent = String.format("[REMINDER] voucherId=%s userId=%s beginTime=%s",
                    voucherId, userIdStr, beginTime);
            if (smsEnabled && StrUtil.isNotBlank(smsTo)) {
                log.info("[REMINDER_SMS] to={} content={}", smsTo, notifyContent);
            }
            if (appEnabled) {
                log.info("[REMINDER_APP] userId={} content={}", userIdStr, notifyContent);
            }
            notifyCount++;
        }
        return notifyCount;
    }
    
    @Override
    public String topic() {
        return SpringUtil.getPrefixDistinctionName() + "-" + DELAY_VOUCHER_REMINDER;
    }
}
