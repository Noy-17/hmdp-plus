package org.javaup.feign;

import org.javaup.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "hmdp-follow-service", contextId = "hmdp-follow-service-internal", path = "/follow")
public interface FollowInternalFeignClient {

    @GetMapping("/followings/{userId}")
    Result<List<Long>> getFollowings(@PathVariable("userId") Long userId);
}
