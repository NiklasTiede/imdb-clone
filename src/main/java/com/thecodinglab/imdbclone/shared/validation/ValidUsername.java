package com.thecodinglab.imdbclone.shared.validation;

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
                  - has at least 2 characters, at most 20 characters
                  - can contain letters, digits, . or _
                  - . and _ cannot be leading, trailing or consecutive
                  """;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
