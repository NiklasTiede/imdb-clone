package com.thecodinglab.imdbclone.media.internal;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MediaInfrastructureSetup implements ApplicationListener<ApplicationReadyEvent> {

  private final MediaServiceImpl mediaService;

  public MediaInfrastructureSetup(MediaServiceImpl mediaService) {
    this.mediaService = mediaService;
  }

  @Override
  public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
    mediaService.setUpBucket();
  }
}
