package com.thecodinglab.imdbclone.account.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import org.springframework.modulith.NamedInterface;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AvailableEmailImpl.class)
@NamedInterface("identity")
public @interface AvailableEmail {

  String message() default "Email is already taken";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
