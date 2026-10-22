package rsnrdk.websec.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.ServletWebRequest;
import rsnrdk.websec.controller.AnnotatedController;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUsernameArgumentResolverTest {

    private final CurrentUsernameArgumentResolver resolver = new CurrentUsernameArgumentResolver();

    /**
     * Sample handler methods used only to obtain MethodParameter instances.
     */
    @SuppressWarnings("unused")
    static class SampleHandlers {
        void annotated(@CurrentUsername String username) {
        }

        void notAnnotated(String username) {
        }

        void annotatedWrongType(@CurrentUsername Long id) {
        }
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static MethodParameter parameterOf(String methodName, Class<?> type) throws NoSuchMethodException {
        Method method = SampleHandlers.class.getDeclaredMethod(methodName, type);
        return new MethodParameter(method, 0);
    }

    private static void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(username, null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")));
    }

    @Test
    void supportsParameter_annotatedStringParameter_returnsTrue() throws Exception {
        // Arrange
        MethodParameter parameter = parameterOf("annotated", String.class);

        // Act + Assert
        assertTrue(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_parameterWithoutAnnotation_returnsFalse() throws Exception {
        // Arrange
        MethodParameter parameter = parameterOf("notAnnotated", String.class);

        // Act + Assert
        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_annotatedNonStringParameter_returnsFalse() throws Exception {
        // Arrange
        MethodParameter parameter = parameterOf("annotatedWrongType", Long.class);

        // Act + Assert
        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void resolveArgument_authenticatedUser_returnsUsernameAsString() throws Exception {
        // Arrange
        authenticateAs("admin");
        MethodParameter parameter = parameterOf("annotated", String.class);
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        // Act
        Object resolved = resolver.resolveArgument(parameter, null, request, null);

        // Assert
        assertEquals(String.class, resolved.getClass());
        assertEquals("admin", resolved);
    }

    @Test
    void resolveArgument_noAuthentication_throwsCredentialsNotFound() throws Exception {
        // Arrange
        MethodParameter parameter = parameterOf("annotated", String.class);
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        // Act + Assert
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> resolver.resolveArgument(parameter, null, request, null));
    }

    @Test
    void resolveArgument_anonymousToken_throwsCredentialsNotFound() throws Exception {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MethodParameter parameter = parameterOf("annotated", String.class);
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        // Act + Assert
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> resolver.resolveArgument(parameter, null, request, null));
    }

    @Test
    void whoAmI_realRequest_usesResolvedUsernameInResponseBody() throws Exception {
        // Arrange
        authenticateAs("superadmin");
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AnnotatedController())
                .setCustomArgumentResolvers(resolver)
                .build();

        // Act + Assert
        mockMvc.perform(get("/api/v1/annotated/whoami"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("superadmin"))
                .andExpect(jsonPath("$.greeting").value("Hello, superadmin!"));
    }
}