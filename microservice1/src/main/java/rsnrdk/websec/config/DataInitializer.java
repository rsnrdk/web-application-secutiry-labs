package rsnrdk.websec.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rsnrdk.websec.entity.Role;
import rsnrdk.websec.entity.User;
import rsnrdk.websec.repository.UserRepository;

/**
 * Lab 5: seeds three users with different roles (USER, ADMIN, SUPERADMIN).
 * Test credentials: user/user123, admin/admin123, superadmin/superadmin123.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createIfMissing("user", "user123", Role.USER);
        createIfMissing("admin", "admin123", Role.ADMIN);
        createIfMissing("superadmin", "superadmin123", Role.SUPERADMIN);
    }

    private void createIfMissing(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build());
    }
}
