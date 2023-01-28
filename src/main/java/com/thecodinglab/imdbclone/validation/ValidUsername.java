package com.thecodinglab.imdbclone.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidUsernameImpl.class)
public @interface ValidUsername {

  String message() default
      """
                  Username has to follow these rules:
                  - has at least 2 letters, at most 30 letters
                  - can contain . or _ between letters
                  """;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
