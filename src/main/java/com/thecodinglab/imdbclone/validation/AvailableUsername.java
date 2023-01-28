package com.thecodinglab.imdbclone.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AvailableUsernameImpl.class)
public @interface AvailableUsername {

  String message() default "Username is already taken";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
