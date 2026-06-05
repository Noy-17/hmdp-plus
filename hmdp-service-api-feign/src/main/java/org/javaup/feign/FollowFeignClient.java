package org.javaup.feign;

import org.javaup.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "hmdp-follow-service", path = "/follow")
public interface FollowFeignClient {

    @GetMapping("/followers/{followUserId}")
    Result<List<Long>> getFollowerIds(@PathVariable("followUserId") Long followUserId);
}
