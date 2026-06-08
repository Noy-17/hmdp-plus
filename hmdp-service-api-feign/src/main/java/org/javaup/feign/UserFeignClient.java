package org.javaup.feign;

import org.javaup.dto.Result;
import org.javaup.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "hmdp-user-service", contextId = "hmdp-user-service", path = "/user")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserDTO> getById(@PathVariable("id") Long id);

    @PostMapping("/batch")
    Result<List<UserDTO>> listByIds(@RequestBody List<Long> ids);
}
