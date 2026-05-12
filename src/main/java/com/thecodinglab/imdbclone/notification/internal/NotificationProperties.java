package com.thecodinglab.imdbclone.notification.internal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "imdb-clone.notification.email")
public record NotificationProperties(@NotBlank String sender) {}
