package org.javaup.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.UserBehaviorEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final String PROFILE_KEY_PREFIX = "user:profile:";
    private static final String RECENT_KEY_PREFIX = "user:behavior:recent:";

    private final StringRedisTemplate stringRedisTemplate;

    public Map<Object, Object> getProfile(Long userId) {
        String key = PROFILE_KEY_PREFIX + userId;
        return stringRedisTemplate.opsForHash().entries(key);
    }

    public void updateProfile(UserBehaviorEvent event) {
        Long userId = event.getUserId();
        String key = PROFILE_KEY_PREFIX + userId;
        String type = event.getEventType();

        switch (type) {
            case "PURCHASE":
                // preferred type increment comes via ViewShop/targetType lookup;
                // here we update avgPrice and favoriteShops from voucher-service context.
                // For now, mark purchase frequency.
                stringRedisTemplate.opsForHash().increment(key, "purchaseFrequency", 1);
                stringRedisTemplate.expire(key, 30, TimeUnit.DAYS);
                break;
            case "VIEW_SHOP":
                if (event.getTargetId() != null) {
                    stringRedisTemplate.opsForHash().increment(key, "shopViews", 1);
                    stringRedisTemplate.expire(key, 30, TimeUnit.DAYS);
                }
                break;
            case "SEARCH":
                if (StrUtil.isNotBlank(event.getTargetType())) {
                    String recentKey = RECENT_KEY_PREFIX + userId;
                    stringRedisTemplate.opsForList().leftPush(recentKey, event.getTargetType());
                    stringRedisTemplate.opsForList().trim(recentKey, 0, 49);
                }
                break;
            case "LIKE_BLOG":
                stringRedisTemplate.opsForHash().increment(key, "blogLikes", 1);
                stringRedisTemplate.expire(key, 30, TimeUnit.DAYS);
                break;
        }
    }

    public String formatForPrompt(Long userId) {
        Map<Object, Object> profile = getProfile(userId);
        if (profile.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Object purchaseFreq = profile.get("purchaseFrequency");
        if (purchaseFreq != null) {
            sb.append("本周购买").append(purchaseFreq).append("次，");
        }
        Object shopViews = profile.get("shopViews");
        if (shopViews != null) {
            sb.append("浏览商铺").append(shopViews).append("次，");
        }
        Object blogLikes = profile.get("blogLikes");
        if (blogLikes != null) {
            sb.append("点赞博客").append(blogLikes).append("次，");
        }

        String recentKey = RECENT_KEY_PREFIX + userId;
        var tags = stringRedisTemplate.opsForList().range(recentKey, 0, 4);
        if (tags != null && !tags.isEmpty()) {
            sb.append("最近搜索:").append(String.join(",", tags)).append("，");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
