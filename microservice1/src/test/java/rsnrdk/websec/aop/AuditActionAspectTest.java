package rsnrdk.websec.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AuditActionAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    private final AuditActionAspect aspect = new AuditActionAspect();
    private AuditAction auditAction;

    /**
     * Holder of a real @AuditAction instance.
     */
    static class Sample {
        @AuditAction("ITEM_DELETED")
        void delete() {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        auditAction = Sample.class.getDeclaredMethod("delete").getAnnotation(AuditAction.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ItemService.deleteItem(..)");
        when(joinPoint.getArgs()).thenReturn(new Object[]{7L});
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void audit_authenticatedUser_logsActionUserMethodAndArgs(CapturedOutput output) {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("admin", null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        // Act
        aspect.audit(joinPoint, auditAction);

        // Assert
        assertTrue(output.getOut().contains(
                "action=ITEM_DELETED user=admin method=ItemService.deleteItem(..) args=[7]"));
    }

    @Test
    void audit_noAuthentication_logsAnonymousUser(CapturedOutput output) {
        // Act
        aspect.audit(joinPoint, auditAction);

        // Assert
        assertTrue(output.getOut().contains("action=ITEM_DELETED user=anonymous"));
    }
}