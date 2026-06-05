package org.javaup.bridge;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.javaup.entity.User;
import org.javaup.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阶段四临时方案：直接通过 MyBatis-Plus 跨服务读取 tb_user 表。
 * FollowService 关注/共同关注时查询用户信息。
 * 阶段五引入 Nacos + OpenFeign 后替换为 Feign 接口调用。
 *
 * @deprecated 阶段五由 OpenFeign 替换
 */
@Component
public class UserBridge extends ServiceImpl<UserMapper, User> {

    public List<User> listByIds(List<Long> ids) {
        return super.listByIds(ids);
    }
}
