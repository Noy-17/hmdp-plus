package org.javaup.feign;

import org.javaup.dto.Result;
import org.javaup.dto.UserInfoDTO;
import org.javaup.dto.LevelQueryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "hmdp-user-service", contextId = "hmdp-user-service-info", path = "/user")
public interface UserInfoFeignClient {

    @GetMapping("/info/{userId}")
    Result<UserInfoDTO> getInfoByUserId(@PathVariable("userId") Long userId);

    @PostMapping("/info/by-levels")
    Result<List<UserInfoDTO>> listByLevels(@RequestBody LevelQueryRequest req);
}
