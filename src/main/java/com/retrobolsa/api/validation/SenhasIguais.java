package com.retrobolsa.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhasIguaisValidator.class)
@Documented
public @interface SenhasIguais {
    String message() default "Senhas não coincidem";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}