package rsnrdk.websec.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import rsnrdk.websec.entity.AuditLog;
import rsnrdk.websec.repository.AuditLogRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditInterceptorTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditInterceptor auditInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("DELETE", "/api/v1/items/7");
        request.setRemoteAddr("10.0.0.5");
        response = new MockHttpServletResponse();
        response.setStatus(403);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void afterCompletion_authenticatedUser_savesAuditLogWithRequestDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("admin", null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        auditInterceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals("admin", log.getUsername());
        assertEquals("DELETE", log.getMethod());
        assertEquals("/api/v1/items/7", log.getUri());
        assertEquals(403, log.getStatus());
        assertEquals("10.0.0.5", log.getIp());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void afterCompletion_noAuthentication_logsAnonymous() {
        auditInterceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("anonymous", captor.getValue().getUsername());
    }

    @Test
    void afterCompletion_springAnonymousToken_logsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        auditInterceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("anonymous", captor.getValue().getUsername());
    }

    @Test
    void afterCompletion_repositoryFails_doesNotPropagateException() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB is down"));

        // Act + Assert
        assertDoesNotThrow(() -> auditInterceptor.afterCompletion(request, response, new Object(), null));
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}