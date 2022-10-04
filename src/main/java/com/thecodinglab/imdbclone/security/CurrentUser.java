package com.thecodinglab.imdbclone.security;

import java.lang.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {}
