package com.thecodinglab.imdbclone.validation;

import java.lang.annotation.*;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidPasswordImpl.class)
public @interface ValidPassword {

  String message() default
      """
            Password has to follow these rules:
            - at least 1 upper case letter
            - at least 1 lower case english letter
            - at least 1 digit
            - at least 1 special character
            - minimum length is 8
            - maximum length is 20
            """;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
