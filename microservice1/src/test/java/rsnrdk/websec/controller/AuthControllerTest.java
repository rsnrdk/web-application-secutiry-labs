package rsnrdk.websec.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import rsnrdk.websec.dto.LoginRequest;
import rsnrdk.websec.dto.RegisterRequest;
import rsnrdk.websec.entity.Role;
import rsnrdk.websec.entity.User;
import rsnrdk.websec.repository.UserRepository;
import rsnrdk.websec.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_newUsername_savesUserWithUserRoleAndEncodedPassword() {
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");

        ResponseEntity<String> response = authController.register(new RegisterRequest("newbie", "secret123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("newbie", captor.getValue().getUsername());
        assertEquals("encoded-secret", captor.getValue().getPassword());
        assertEquals(Role.USER, captor.getValue().getRole());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
    }

    @Test
    void register_existingUsername_throwsAndDoesNotSave() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authController.register(new RegisterRequest("admin", "secret123"))
        );

        assertEquals("Username already taken", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsJwtToken() {
        User user = User.builder().username("admin").password("encoded").role(Role.ADMIN).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        ResponseEntity<String> response = authController.login(new LoginRequest("admin", "admin123"));

        verify(authenticationManager).authenticate(any());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", response.getBody());
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsAndIssuesNoToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // Act + Assert
        assertThrows(BadCredentialsException.class,
                () -> authController.login(new LoginRequest("admin", "wrong")));
        verify(jwtService, never()).generateToken(any());
    }
}