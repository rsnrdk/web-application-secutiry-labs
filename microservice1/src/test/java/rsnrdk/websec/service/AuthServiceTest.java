package rsnrdk.websec.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import rsnrdk.websec.dto.AuthResponse;
import rsnrdk.websec.dto.LoginRequest;
import rsnrdk.websec.dto.RegisterRequest;
import rsnrdk.websec.entity.Role;
import rsnrdk.websec.entity.User;
import rsnrdk.websec.repository.UserRepository;
import rsnrdk.websec.security.JwtService;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_newUsername_savesUserWithEncodedPasswordAndReturnsToken() {
        // Arrange
        RegisterRequest request = new RegisterRequest("arsenii", "secret123");
        when(userRepository.existsByUsername("arsenii")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("arsenii", saved.getUsername());
        assertEquals("encoded-secret", saved.getPassword());
        assertEquals("jwt-token", response.token());
        assertEquals("arsenii", response.username());
    }

    @Test
    void register_newUsername_assignsDefaultUserRole() {
        RegisterRequest request = new RegisterRequest("arsenii", "secret123");
        when(userRepository.existsByUsername("arsenii")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");


        authService.register(request);


        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.USER, captor.getValue().getRole());
    }

    @Test
    void register_existingUsername_throwsAndDoesNotSave() {

        RegisterRequest request = new RegisterRequest("arsenii", "secret123");
        when(userRepository.existsByUsername("arsenii")).thenReturn(true);


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Username already taken", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void login_validCredentials_authenticatesAndReturnsToken() {
        LoginRequest request = new LoginRequest("arsenii", "secret123");
        User user = User.builder().username("arsenii").password("encoded").role(Role.USER).build();
        when(userRepository.findByUsername("arsenii")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("arsenii", captor.getValue().getPrincipal());
        assertEquals("secret123", captor.getValue().getCredentials());
        assertEquals("jwt-token", response.token());
        assertEquals("arsenii", response.username());
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsAndIssuesNoToken() {
        LoginRequest request = new LoginRequest("arsenii", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act + Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_userMissingAfterAuthentication_throwsNoSuchElement() {
        LoginRequest request = new LoginRequest("ghost", "secret123");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
    }
}