package org.javaup.service.impl;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.IAutoIssueNotifyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AutoIssueNotifyServiceImpl implements IAutoIssueNotifyService {

    @Value("${seckill.autoissue.notify.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${seckill.autoissue.notify.app.enabled:false}")
    private boolean appEnabled;

    @Value("${seckill.autoissue.notify.sms.to:}")
    private String smsTo;

    @Value("${seckill.autoissue.notify.dedup.window.seconds:300}")
    private long dedupWindowSeconds;

    @Resource
    private RedisCache redisCache;

    @Override
    public void sendAutoIssueNotify(Long voucherId, Long userId, Long orderId) {
        try {
            if (!shouldNotify(voucherId, userId)) {
                return;
            }
            String content = String.format("自动发券成功 | voucherId=%s userId=%s orderId=%s", voucherId, userId, orderId);
            if (smsEnabled && StrUtil.isNotBlank(smsTo)) {
                log.info("[AUTOISSUE_SMS] to={} content={}", smsTo, content);
            }
            if (appEnabled) {
                log.info("[AUTOISSUE_APP] userId={} content={}", userId, content);
            }
        } catch (Exception e) {
            log.warn("发送自动发券通知异常", e);
        }
    }

    private boolean shouldNotify(Long voucherId, Long userId) {
        try {
            return redisCache.setIfAbsent(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_AUTO_ISSUE_NOTIFY_DEDUP_KEY, voucherId, userId),
                    "1",
                    dedupWindowSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            return true;
        }
    }
}