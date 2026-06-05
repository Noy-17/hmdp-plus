package org.javaup.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.dto.Result;
import org.javaup.entity.UserInfo;
import org.javaup.enums.BaseCode;
import org.javaup.exception.HmdpFrameException;
import org.javaup.mapper.UserInfoMapper;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.IUserInfoService;
import org.javaup.servicelock.LockType;
import org.javaup.servicelock.annotion.ServiceLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.javaup.constant.DistributedLockConstants.UPDATE_USER_INFO_LOCK;

@Slf4j
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Resource
    private RedisCache redisCache;
    
    @Override
    @ServiceLock(lockType= LockType.Read,name = UPDATE_USER_INFO_LOCK,keys = {"#userId"})
    public UserInfo getByUserId(Long userId){
        UserInfo userInfo = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_INFO_KEY, userId), UserInfo.class);
        if (Objects.nonNull(userInfo)){
            return userInfo;
        }
        userInfo = lambdaQuery().eq(UserInfo::getUserId, userId).one();
        if (Objects.isNull(userInfo)) {
            throw new HmdpFrameException(BaseCode.USER_NOT_EXIST);
        }
        redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_INFO_KEY, userId), userInfo);
        return userInfo;
    }
    
    @Override
    @ServiceLock(lockType= LockType.Write,name = UPDATE_USER_INFO_LOCK,keys = {"#userId"})
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateUserLevel(Long userId, Integer newLevel) {
        if (Objects.isNull(userId) || Objects.isNull(newLevel) || newLevel <= 0) {
            return Result.fail("参数非法：userId/newLevel");
        }
        UserInfo userInfo = this.lambdaQuery()
                .eq(UserInfo::getUserId, userId)
                .one();
        if (Objects.isNull(userInfo)) {
            return Result.fail("用户信息不存在");
        }
        Integer oldLevel = userInfo.getLevel();
        if (Objects.equals(oldLevel, newLevel)) {
            return Result.ok();
        }
        // 更新数据库等级
        boolean updated = this.lambdaUpdate()
                .set(UserInfo::getLevel, newLevel)
                .eq(UserInfo::getUserId, userId)
                .update();
        if (!updated) {
            return Result.fail("更新等级失败");
        }
        // 删除用户信息缓存
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_INFO_KEY, userId));
        // 维护Redis集合倒排索引（best-effort，不影响事务提交）
        try {
            if (Objects.nonNull(oldLevel) && oldLevel > 0) {
                redisCache.removeForSet(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_LEVEL_MEMBERS_TAG_KEY, oldLevel),
                        userId
                );
            }
            redisCache.addForSet(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_LEVEL_MEMBERS_TAG_KEY, newLevel),
                    userId
            );
        } catch (Exception e) {
            // 记录日志但不回滚业务事务
            log.error("维护用户等级集合失败 userId={} oldLevel={} newLevel={}", userId, oldLevel, newLevel, e);
        }
        return Result.ok();
    }

}
