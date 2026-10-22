package rsnrdk.websec.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private final RetryAspect aspect = new RetryAspect();
    private RetryOnFailure retry;

    /**
     * Holder of a real @RetryOnFailure instance with default settings (3 attempts, transient DB errors).
     */
    static class Sample {
        @RetryOnFailure
        void read() {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        retry = Sample.class.getDeclaredMethod("read").getAnnotation(RetryOnFailure.class);
        // signature is only needed for the warning log on a retry
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("ItemService.getItem(..)");
    }

    @Test
    void retry_firstAttemptSucceeds_proceedsOnce() throws Throwable {
        // Arrange
        when(joinPoint.proceed()).thenReturn("item");

        // Act
        Object result = aspect.retry(joinPoint, retry);

        // Assert
        assertEquals("item", result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void retry_transientFailureThenSuccess_returnsResultOfSecondAttempt() throws Throwable {
        // Arrange
        when(joinPoint.proceed())
                .thenThrow(new QueryTimeoutException("DB timeout"))
                .thenReturn("item");

        // Act
        Object result = aspect.retry(joinPoint, retry);

        // Assert
        assertEquals("item", result);
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void retry_transientFailureEveryTime_givesUpAfterMaxAttemptsAndRethrows() throws Throwable {
        // Arrange
        QueryTimeoutException failure = new QueryTimeoutException("DB timeout");
        when(joinPoint.proceed()).thenThrow(failure);

        // Act
        QueryTimeoutException thrown = assertThrows(
                QueryTimeoutException.class,
                () -> aspect.retry(joinPoint, retry)
        );

        // Assert
        assertSame(failure, thrown);
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void retry_nonTransientFailure_isNotRetried() throws Throwable {
        // Arrange: "item not found" is a business answer, retrying it would be pointless
        when(joinPoint.proceed())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        // Act
        assertThrows(ResponseStatusException.class, () -> aspect.retry(joinPoint, retry));

        // Assert
        verify(joinPoint, times(1)).proceed();
    }
}