package rsnrdk.websec.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void handleIllegalArgument_duplicateUsername_returnsConflictWithMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("Username already taken");

        ProblemDetail result = handler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals("Username already taken", result.getDetail());
    }
}