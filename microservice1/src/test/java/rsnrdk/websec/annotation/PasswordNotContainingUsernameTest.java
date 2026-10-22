package rsnrdk.websec.annotation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rsnrdk.websec.dto.RegisterRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordNotContainingUsernameTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        // Bean Validation discovers PasswordNotContainingUsernameValidator through @Constraint(validatedBy = ...)
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validate_passwordUnrelatedToUsername_noViolations() {
        RegisterRequest request = new RegisterRequest("arsenii", "Str0ngP3n1$Passw0rd");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_passwordContainsUsername_reportsViolationOnPasswordField() {
        RegisterRequest request = new RegisterRequest("arsenii", "arsenii2005");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertEquals("password", violation.getPropertyPath().toString());
        assertEquals("Password must not contain the username", violation.getMessage());
    }

    @Test
    void validate_passwordContainsUsernameInDifferentCase_reportsViolation() {
        RegisterRequest request = new RegisterRequest("Arsenii", "my-ARSENII-pass");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Password must not contain the username", violations.iterator().next().getMessage());
    }

    @Test
    void validate_blankUsername_onlyStandardConstraintsReport() {
        RegisterRequest request = new RegisterRequest("", "whatever_1337_420");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Assert: @NotBlank and @Size complain about username, our rule stays silent
        assertTrue(violations.stream()
                .noneMatch(v -> v.getMessage().equals("Password must not contain the username")));
        assertTrue(violations.stream()
                .allMatch(v -> v.getPropertyPath().toString().equals("username")));
    }
}