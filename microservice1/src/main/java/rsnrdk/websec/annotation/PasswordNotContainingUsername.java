package rsnrdk.websec.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint: the password must not contain the username (case-insensitive).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordNotContainingUsernameValidator.class)
public @interface PasswordNotContainingUsername {

    String message() default "Password must not contain the username";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}