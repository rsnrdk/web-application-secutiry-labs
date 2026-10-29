package rsnrdk.websec.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import rsnrdk.websec.dto.LoginRequest;
import rsnrdk.websec.dto.RegisterRequest;
import rsnrdk.websec.entity.Role;
import rsnrdk.websec.entity.User;
import rsnrdk.websec.repository.UserRepository;
import rsnrdk.websec.security.JwtService;
import rsnrdk.websec.aop.RateLimited;
import rsnrdk.websec.async.NotificationService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        // fire-and-forget: the response does not wait for the email
        try {
            notificationService.sendWelcomeEmail(user.getUsername());
        } catch (TaskRejectedException e) {
            // executor is full: registration already succeeded, so we only skip the email instead of failing
            log.warn("Notification executor is full, welcome email for {} was not queued", user.getUsername());
        }

        return ResponseEntity.ok("User registered successfully");
    }

    @RateLimited(maxCalls = 10, windowSeconds = 60)
    @PostMapping("/login")
    public ResponseEntity<String> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        UserDetails userDetails = userRepository
                .findByUsername(loginRequest.username())
                .orElseThrow();

        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(token);
    }
}