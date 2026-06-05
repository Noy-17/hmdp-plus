package org.javaup.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.dto.LoginFormDTO;
import org.javaup.dto.Result;
import org.javaup.dto.UserDTO;
import org.javaup.entity.User;
import org.javaup.entity.UserInfo;
import org.javaup.entity.UserPhone;
import org.javaup.mapper.UserMapper;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.IUserInfoService;
import org.javaup.service.IUserPhoneService;
import org.javaup.service.IUserService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.javaup.utils.RegexUtils;
import org.javaup.utils.UserHolder;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.javaup.utils.RedisConstants.LOGIN_CODE_KEY;
import static org.javaup.utils.RedisConstants.LOGIN_CODE_TTL;
import static org.javaup.utils.RedisConstants.LOGIN_USER_KEY;
import static org.javaup.utils.RedisConstants.LOGIN_USER_TTL;
import static org.javaup.utils.RedisConstants.USER_SIGN_KEY;
import static org.javaup.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    @Resource
    private IUserInfoService userInfoService;
    
    @Resource
    private IUserPhoneService userPhoneService;

    @Resource
    private RedisCache redisCache;

    @Override
    public Result<String> sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到 session
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码
        log.info("发送短信验证码成功，验证码：{}", code);
        // 返回ok
        return Result.ok(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.从redis获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            return Result.fail("验证码错误");
        }

        // 4.根据手机号查询用户
        UserPhone userPhone = userPhoneService.lambdaQuery().eq(UserPhone::getPhone, phone).one();

        User user = null;
        // 5.判断用户是否存在
        if (userPhone == null) {
            // 6.不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }else {
            user = lambdaQuery().eq(User::getPhone, userPhone.getPhone()).one();
        }

        // 7.保存用户信息到 redis中
        // 7.1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 7.2.将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 7.3.存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 7.4.设置token有效期（按秒设置，避免 Redisson pExpire 递归问题）
        stringRedisTemplate.expire(
                tokenKey,
                TimeUnit.SECONDS.convert(LOGIN_USER_TTL, TimeUnit.MINUTES),
                TimeUnit.SECONDS
        );

        // 8.返回token
        try {
            maintainLevelSetMembership(user.getId());
        } catch (Exception e) {
            // 忽略异常，避免影响登录
        }
        return Result.ok(token);
    }

    @Override
    public Result<Void> sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result<Integer> signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }
    
    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setId(snowflakeIdGenerator.nextId());
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        // 3.保存用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setId(snowflakeIdGenerator.nextId());
        userInfo.setUserId(user.getId());
        userInfo.setLevel(1);
        userInfoService.save(userInfo);
        try {
            maintainLevelSetMembership(user.getId());
        } catch (Exception e) {
            // 忽略异常，避免影响注册逻辑
        }
        // 4.保存用户手机信息
        UserPhone userPhone = new UserPhone();
        userPhone.setId(snowflakeIdGenerator.nextId());
        userPhone.setUserId(user.getId());
        userPhone.setPhone(phone);
        userPhoneService.save(userPhone);
        return user;
    }
    
    private void maintainLevelSetMembership(Long userId) {
        if (userId == null) {
            return;
        }
        UserInfo info = userInfoService.lambdaQuery().eq(UserInfo::getUserId, userId).one();
        if (info == null || info.getLevel() == null || info.getLevel() <= 0) {
            return;
        }
        Integer level = info.getLevel();
        redisCache.addForSet(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_LEVEL_MEMBERS_TAG_KEY, level),
                userId
        );
    }
}
