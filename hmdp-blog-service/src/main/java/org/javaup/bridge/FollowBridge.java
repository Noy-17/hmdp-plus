package org.javaup.bridge;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.javaup.entity.Follow;
import org.javaup.mapper.FollowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阶段四临时方案：直接通过 MyBatis-Plus 跨服务读取 tb_follow 表。
 * BlogService 发布笔记后查询作者粉丝列表推送 Feed 时调用。
 * 阶段五引入 Nacos + OpenFeign 后替换为 Feign 接口调用。
 *
 * @deprecated 阶段五由 OpenFeign 替换
 */
@Component
public class FollowBridge extends ServiceImpl<FollowMapper, Follow> {

    public List<Follow> getFollowers(Long followUserId) {
        return lambdaQuery().eq(Follow::getFollowUserId, followUserId).list();
    }
}
