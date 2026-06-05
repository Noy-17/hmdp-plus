package org.javaup.controller;


import cn.hutool.core.bean.BeanUtil;
import org.javaup.dto.LoginFormDTO;
import org.javaup.dto.Result;
import org.javaup.dto.UserDTO;
import org.javaup.entity.User;
import org.javaup.entity.UserInfo;
import org.javaup.service.IUserInfoService;
import org.javaup.service.IUserService;
import org.javaup.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result<String> sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result<Void> logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result<UserDTO> me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result<UserInfo> info(@PathVariable("id") String userId){
        // 查询详情
        UserInfo info = userInfoService.getById(Long.parseLong(userId));
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 当前登录用户更新等级
     */
    @PostMapping("/level/update")
    public Result<Void> updateLevel(@RequestParam("newLevel") Integer newLevel) {
        UserDTO current = UserHolder.getUser();
        if (Objects.isNull(current)) {
            return Result.fail("未登录");
        }
        return userInfoService.updateUserLevel(current.getId(), newLevel);
    }

    @GetMapping("/{id}")
    public Result<UserDTO> queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    public Result<Void> sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}