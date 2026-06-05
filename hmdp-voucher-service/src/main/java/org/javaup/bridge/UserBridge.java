package org.javaup.bridge;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.javaup.entity.UserInfo;
import org.javaup.mapper.UserInfoMapper;
import org.springframework.stereotype.Component;

/**
 * 阶段四临时方案：直接通过 MyBatis-Plus 跨服务读取 tb_user_info 表。
 * 阶段五引入 Nacos + OpenFeign 后替换为 Feign 接口调用。
 *
 * @deprecated 阶段五由 OpenFeign 替换
 */
@Component
public class UserBridge extends ServiceImpl<UserInfoMapper, UserInfo> {

    public UserInfo getByUserId(Long userId) {
        return lambdaQuery().eq(UserInfo::getUserId, userId).one();
    }
}
