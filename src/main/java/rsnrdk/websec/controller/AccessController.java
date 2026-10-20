package rsnrdk.websec.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab 5: access to these routes is restricted by URL rules in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/access")
public class AccessController {

    @GetMapping("/public")
    public String publicAccess() {
        return "Public access granted";
    }

    @GetMapping("/user")
    public String userAccess() {
        return "User access granted";
    }

    @GetMapping("/admin")
    public String adminAccess() {
        return "Admin access granted";
    }

    @GetMapping("/superadmin")
    public String superAdminAccess() {
        return "Superadmin access granted";
    }
}
