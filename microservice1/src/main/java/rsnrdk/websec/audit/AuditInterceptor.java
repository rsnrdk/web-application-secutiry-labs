package rsnrdk.websec.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import rsnrdk.websec.entity.AuditLog;
import rsnrdk.websec.repository.AuditLogRepository;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName()
                : "anonymous";

        AuditLog log = AuditLog.builder()
                .username(username)
                .method(request.getMethod())
                .uri(request.getRequestURI())
                .status(response.getStatus())
                .ip(request.getRemoteAddr())
                .createdAt(Instant.now())
                .build();

        try {
            auditLogRepository.save(log);
        } catch (Exception ignored) {

        }
    }
}