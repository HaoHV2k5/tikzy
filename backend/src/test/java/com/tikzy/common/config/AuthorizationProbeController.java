package com.tikzy.common.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationProbeController {

    @GetMapping("/api/v1/admin/probe")
    String admin() {
        return "admin";
    }

    @GetMapping("/api/v1/organizer/probe")
    String organizer() {
        return "organizer";
    }

    @GetMapping("/api/v1/customer/probe")
    String customer() {
        return "customer";
    }

    @GetMapping("/api/v1/users/probe")
    String user() {
        return "user";
    }
}
