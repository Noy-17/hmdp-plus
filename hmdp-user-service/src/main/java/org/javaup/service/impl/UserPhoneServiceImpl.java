package org.javaup.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.javaup.entity.UserPhone;
import org.javaup.mapper.UserPhoneMapper;
import org.javaup.service.IUserPhoneService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserPhoneServiceImpl extends ServiceImpl<UserPhoneMapper, UserPhone> implements IUserPhoneService {
    
}
