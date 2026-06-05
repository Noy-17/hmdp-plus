package org.javaup.service;

import org.javaup.dto.Result;
import org.javaup.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);

    Result<List<Long>> getFollowerIds(Long followUserId);
}
