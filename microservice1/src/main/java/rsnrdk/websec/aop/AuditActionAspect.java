package rsnrdk.websec.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class AuditActionAspect {

    // separate logger name, so audit events can be routed to their own file or system
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    @AfterReturning("@annotation(auditAction)")
    public void audit(JoinPoint joinPoint, AuditAction auditAction) {
        AUDIT.info("action={} user={} method={} args={}",
                auditAction.value(),
                currentUser(),
                joinPoint.getSignature().toShortString(),
                Arrays.toString(joinPoint.getArgs()));
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        return auth.getName();
    }
}