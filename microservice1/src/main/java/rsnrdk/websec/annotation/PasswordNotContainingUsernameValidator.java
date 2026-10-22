package rsnrdk.websec.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import rsnrdk.websec.dto.RegisterRequest;

import java.util.Locale;

public class PasswordNotContainingUsernameValidator
        implements ConstraintValidator<PasswordNotContainingUsername, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null || request.username() == null || request.password() == null) {
            // missing values are reported by @NotBlank, not by this rule
            return true;
        }

        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String password = request.password().toLowerCase(Locale.ROOT);
        if (username.isEmpty() || !password.contains(username)) {
            return true;
        }

        // attach the violation to the "password" field so clients know which input to fix
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("password")
                .addConstraintViolation();
        return false;
    }
}