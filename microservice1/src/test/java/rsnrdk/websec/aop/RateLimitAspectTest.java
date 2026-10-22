package rsnrdk.websec.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private Clock clock;

    private RateLimitAspect aspect;
    private RateLimited limit;

    /**
     * Holder of a real @RateLimited instance: annotations cannot be created with "new".
     */
    static class Sample {
        @RateLimited(maxCalls = 2, windowSeconds = 60)
        void limited() {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        aspect = new RateLimitAspect(clock);
        limit = Sample.class.getDeclaredMethod("limited").getAnnotation(RateLimited.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AuthController.login(..)");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void requestFrom(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void limit_underLimit_proceedsAndReturnsResult() throws Throwable {
        // Arrange
        when(clock.instant()).thenReturn(T0);
        when(joinPoint.proceed()).thenReturn("jwt-token");

        // Act
        Object result = aspect.limit(joinPoint, limit);

        // Assert
        assertEquals("jwt-token", result);
        verify(joinPoint).proceed();
    }

    @Test
    void limit_limitReached_throws429WithoutCallingMethod() throws Throwable {
        // Arrange
        when(clock.instant()).thenReturn(T0, T0.plusSeconds(1), T0.plusSeconds(2));
        when(joinPoint.proceed()).thenReturn("jwt-token");
        aspect.limit(joinPoint, limit);
        aspect.limit(joinPoint, limit);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> aspect.limit(joinPoint, limit)
        );

        // Assert: third call is rejected and the real method is NOT executed
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void limit_windowExpired_allowsCallsAgain() throws Throwable {
        // Arrange
        when(clock.instant()).thenReturn(T0, T0.plusSeconds(1), T0.plusSeconds(61));
        when(joinPoint.proceed()).thenReturn("jwt-token");
        aspect.limit(joinPoint, limit);
        aspect.limit(joinPoint, limit);

        // Act: 61 seconds later the first call has left the 60s window
        Object result = aspect.limit(joinPoint, limit);

        // Assert
        assertEquals("jwt-token", result);
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void limit_differentClients_haveSeparateLimits() throws Throwable {
        // Arrange
        when(clock.instant()).thenReturn(T0);
        when(joinPoint.proceed()).thenReturn("jwt-token");
        requestFrom("10.0.0.1");
        aspect.limit(joinPoint, limit);
        aspect.limit(joinPoint, limit);

        // Act: client 10.0.0.1 is exhausted, but 10.0.0.2 has its own budget
        requestFrom("10.0.0.2");
        Object result = aspect.limit(joinPoint, limit);

        // Assert
        assertEquals("jwt-token", result);
        verify(joinPoint, times(3)).proceed();
    }
}