package com.example.mscourse.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-auth", url = "${MS_AUTH_URL:http://localhost:8081}")
public interface UserClient {

    @GetMapping("/api/users/by-email")
    Long getUserIdByEmail(@RequestParam String email);
}