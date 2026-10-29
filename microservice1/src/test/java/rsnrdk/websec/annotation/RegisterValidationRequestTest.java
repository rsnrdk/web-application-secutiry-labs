package rsnrdk.websec.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rsnrdk.websec.async.NotificationService;
import rsnrdk.websec.controller.AuthController;
import rsnrdk.websec.controller.RestExceptionHandler;
import rsnrdk.websec.repository.UserRepository;
import rsnrdk.websec.security.JwtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegisterValidationRequestTest {

    private UserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        AuthController controller = new AuthController(
                mock(AuthenticationManager.class),
                userRepository,
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(NotificationService.class)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void register_passwordContainsUsername_returns400WithClearMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"arsenii\",\"password\":\"arsenii2005\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must not contain the username"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_validPassword_passesValidationAndSavesUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"arsenii\",\"password\":\"Str0ngP3n1$Passw0rd\"}"))
                .andExpect(status().isOk());

        verify(userRepository).save(any());
    }
}