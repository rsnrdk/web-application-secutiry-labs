package rsnrdk.websec.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final Clock clock;
    private final Map<String, Deque<Instant>> callsByKey = new ConcurrentHashMap<>();

    public RateLimitAspect() {
        this(Clock.systemUTC());
    }

    RateLimitAspect(Clock clock) {
        this.clock = clock;
    }

    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = joinPoint.getSignature().toShortString() + "|" + clientKey();
        Instant now = clock.instant();
        Instant windowStart = now.minusSeconds(rateLimited.windowSeconds());

        Deque<Instant> calls = callsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (calls) {
            // forget calls that are older than the window
            while (!calls.isEmpty() && !calls.peekFirst().isAfter(windowStart)) {
                calls.pollFirst();
            }
            if (calls.size() >= rateLimited.maxCalls()) {
                log.warn("Rate limit exceeded for {}: {} calls in {}s", key, calls.size(), rateLimited.windowSeconds());
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, try again later");
            }
            calls.addLast(now);
        }
        return joinPoint.proceed();
    }

    private String clientKey() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return "no-request";
    }
}