package com.tikzy.common.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class AuthorizationProbeController {

    @GetMapping("/api/v1/admin/probe")
    @PreAuthorize("hasRole('ADMIN')")
    String admin() {
        return "admin";
    }

    @GetMapping("/api/v1/organizer/probe")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    String organizer() {
        return "organizer";
    }

    @GetMapping("/api/v1/customer/probe")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    String customer() {
        return "customer";
    }

    @GetMapping("/api/v1/users/probe")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ORGANIZER', 'ADMIN')")
    String user() {
        return "user";
    }
}
