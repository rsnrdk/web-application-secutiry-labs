package rsnrdk.websec.controller;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lab 6: access to these routes is restricted by method security annotations.
 */
@RestController
@RequestMapping("/api/v1/annotated")
public class AnnotatedController {

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public Map<String, Object> profile(Authentication authentication) {
        return Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        );
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPERADMIN')")
    @GetMapping("/user")
    public String user() {
        return "User access granted via @PreAuthorize";
    }

    @Secured({"ROLE_ADMIN", "ROLE_SUPERADMIN"})
    @GetMapping("/admin")
    public String admin() {
        return "Admin access granted via @Secured";
    }

    @RolesAllowed("SUPERADMIN")
    @GetMapping("/superadmin")
    public String superAdmin() {
        return "Superadmin access granted via @RolesAllowed";
    }

    @PreAuthorize("#username == authentication.name or hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/users/{username}")
    public String userDetails(@PathVariable("username") String username) {
        return "Details of user " + username;
    }
}
