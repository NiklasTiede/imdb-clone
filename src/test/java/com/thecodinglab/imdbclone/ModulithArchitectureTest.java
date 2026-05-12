package com.thecodinglab.imdbclone;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.account.api.AccountService;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.engagement.api.CommentService;
import com.thecodinglab.imdbclone.engagement.api.EngagementStatsService;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieService;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

  @Test
  void verifiesApplicationModules() {
    ApplicationModules.of(Application.class).verify();
  }

  @Test
  void detectsCatalogModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("catalog")).isPresent();
  }

  @Test
  void detectsIdentityModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("identity")).isPresent();
  }

  @Test
  void detectsAccountModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("account")).isPresent();
  }

  @Test
  void detectsEngagementModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("engagement")).isPresent();
  }

  @Test
  void catalogApiDoesNotExposeInternalTypes() {
    assertThat(Arrays.stream(MovieService.class.getMethods()).flatMap(this::methodTypes))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".catalog.internal."));
  }

  @Test
  void identityApiDoesNotExposeInternalTypes() {
    assertThat(Arrays.stream(AuthenticationService.class.getMethods()).flatMap(this::methodTypes))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".identity.internal."));
  }

  @Test
  void accountApiDoesNotExposeInternalTypes() {
    assertThat(apiTypes(AccountService.class, AccountIdentityService.class))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".account.internal."));
  }

  @Test
  void engagementApiDoesNotExposeInternalTypes() {
    assertThat(
            apiTypes(
                CommentService.class,
                RatingService.class,
                WatchedMovieService.class,
                EngagementStatsService.class))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".engagement.internal."));
  }

  @Test
  void movieSearchBelongsToCatalogModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.catalog.web.SearchController"))
        .isPresent();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/controller/SearchController.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/ElasticSearchService.java")))
        .isFalse();
  }

  @Test
  void authenticationBelongsToIdentityModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.identity.web.AuthenticationController"))
        .isPresent();
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.identity.internal.security.WebSecurityConfig"))
        .isPresent();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/controller/AuthenticationController.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/AuthenticationService.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/security/JwtTokenProvider.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/config/WebSecurityConfig.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/entity/VerificationToken.java")))
        .isFalse();
  }

  @Test
  void accountManagementBelongsToAccountModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.web.AccountController"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.internal.AccountServiceImpl"))
        .isPresent();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/controller/AccountController.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/service/AccountService.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/service/RoleService.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/impl/AccountServiceImpl.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/impl/RoleServiceImpl.java")))
        .isFalse();
  }

  @Test
  void engagementBelongsToEngagementModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.web.CommentController"))
        .isPresent();
    assertThat(
            classIfPresent("com.thecodinglab.imdbclone.engagement.web.AccountEngagementController"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.web.RatingController"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.web.WatchedMovieController"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.internal.CommentServiceImpl"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.internal.RatingServiceImpl"))
        .isPresent();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/controller/CommentController.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/controller/RatingController.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/controller/WatchedMovieController.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/service/CommentService.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/service/RatingService.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/WatchedMovieService.java")))
        .isFalse();
  }

  private Stream<Class<?>> methodTypes(Method method) {
    return Stream.concat(
        Stream.of(method.getReturnType()), Arrays.stream(method.getParameterTypes()));
  }

  private Stream<Class<?>> apiTypes(Class<?>... apiTypes) {
    return Arrays.stream(apiTypes)
        .flatMap(type -> Arrays.stream(type.getMethods()))
        .flatMap(this::methodTypes);
  }

  private Optional<Class<?>> classIfPresent(String className) {
    try {
      return Optional.of(Class.forName(className));
    } catch (ClassNotFoundException ex) {
      return Optional.empty();
    }
  }
}
