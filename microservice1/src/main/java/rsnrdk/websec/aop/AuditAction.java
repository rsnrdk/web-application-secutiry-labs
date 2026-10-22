package rsnrdk.websec.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Records a business audit event (who did what) after the method completes successfully.
 * Handled by {@link AuditActionAspect}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {

    /**
     * Business name of the action, e.g. "ITEM_DELETED".
     */
    String value();
}