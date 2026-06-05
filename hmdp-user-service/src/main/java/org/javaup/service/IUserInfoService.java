package org.javaup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.javaup.dto.Result;
import org.javaup.entity.UserInfo;

public interface IUserInfoService extends IService<UserInfo> {
    
    /**
     * 通过用户id查询用户信息
     * @param userId 用户ID
     * @return 结果
     */
    UserInfo getByUserId(Long userId);
    
    /**
     * 更新用户等级，并维护等级倒排索引集合
     * @param userId 用户ID
     * @param newLevel 新等级
     * @return 结果
     */
    Result<Void> updateUserLevel(Long userId, Integer newLevel);

}
