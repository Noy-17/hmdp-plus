package org.javaup.controller;


import jakarta.annotation.Resource;
import org.javaup.dto.Result;
import org.javaup.service.IFollowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id){
        return followService.followCommons(id);
    }

    @GetMapping("/followers/{followUserId}")
    public Result<List<Long>> getFollowerIds(@PathVariable("followUserId") Long followUserId) {
        return followService.getFollowerIds(followUserId);
    }

    @GetMapping("/followings/{userId}")
    public Result<List<Long>> getFollowings(@PathVariable("userId") Long userId) {
        return followService.getFollowings(userId);
    }
}
