package com.thecodinglab.imdbclone;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.account.api.AccountService;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.engagement.api.CommentService;
import com.thecodinglab.imdbclone.engagement.api.EngagementStatsService;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieService;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.media.api.MediaService;
import com.thecodinglab.imdbclone.notification.api.NotificationService;
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
  void detectsMediaModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("media")).isPresent();
  }

  @Test
  void detectsNotificationModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("notification"))
        .isPresent();
  }

  @Test
  void detectsSharedModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("shared")).isPresent();
  }

  @Test
  void detectsRecommendationModule() {
    assertThat(ApplicationModules.of(Application.class).getModuleByName("recommendation"))
        .isPresent();
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
    assertThat(
            apiTypes(AccountService.class, AccountIdentityService.class, AccountImageService.class))
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
  void mediaApiDoesNotExposeInternalTypes() {
    assertThat(apiTypes(MediaService.class))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".media.internal."));
  }

  @Test
  void notificationApiDoesNotExposeInternalTypes() {
    assertThat(apiTypes(NotificationService.class))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".notification.internal."));
  }

  @Test
  void sharedKernelTypesBelongToSharedModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.api.MessageResponse")).isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.api.PagedResponse")).isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.security.CurrentUser"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.security.UserPrincipal"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.validation.Pagination"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.validation.ValidPassword"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.validation.ValidUsername"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.persistence.DateAudit"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.persistence.CreatedAtAudit"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.error.NotFoundException"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.logging.Log")).isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.payload.MessageResponse")).isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.security.UserPrincipal")).isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.validation.Pagination")).isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.entity.audit.DateAudit")).isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.exception.domain.NotFoundException"))
        .isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.utility.Log")).isEmpty();
  }

  @Test
  void sharedApiDoesNotExposeInternalTypes() {
    assertThat(
            Stream.of(
                    "com.thecodinglab.imdbclone.shared.api.MessageResponse",
                    "com.thecodinglab.imdbclone.shared.api.PagedResponse",
                    "com.thecodinglab.imdbclone.shared.security.UserPrincipal")
                .map(this::classIfPresent)
                .flatMap(Optional::stream)
                .flatMap(type -> Arrays.stream(type.getMethods()))
                .flatMap(this::methodTypes))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".shared.internal."));
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
  void catalogDomainTypesBelongToCatalogModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.catalog.api.MovieGenre")).isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.catalog.api.MovieType")).isPresent();
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.catalog.internal.persistence.MovieGenreConverter"))
        .isPresent();
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.catalog.internal.persistence.MovieGenreConverterImpl"))
        .isPresent();
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.catalog.internal.persistence.StartYearConverter"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.enums.MovieGenreEnum")).isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.enums.MovieTypeEnum")).isEmpty();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/enums/MovieGenreEnum.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/enums/MovieTypeEnum.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/enums/attributeconverter/MovieGenreConverter.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/enums/attributeconverter/MovieGenreConverterImpl.java")))
        .isFalse();
    assertThat(Files.exists(Path.of("src/main/java/com/thecodinglab/imdbclone/enums"))).isFalse();
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
  void accountPersistenceBelongsToAccountModule() throws Exception {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.internal.persistence.Account"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.internal.persistence.Role"))
        .isPresent();
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository"))
        .isPresent();
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.account.internal.persistence.RoleRepository"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.internal.persistence.RoleName"))
        .isPresent();
    assertThat(
            Files.exists(Path.of("src/main/java/com/thecodinglab/imdbclone/entity/Account.java")))
        .isFalse();
    assertThat(Files.exists(Path.of("src/main/java/com/thecodinglab/imdbclone/entity/Role.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/repository/AccountRepository.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/repository/RoleRepository.java")))
        .isFalse();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.enums.RoleNameEnum")).isEmpty();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/enums/RoleNameEnum.java")))
        .isFalse();
    try (Stream<Path> engagementSources =
        Files.walk(Path.of("src/main/java/com/thecodinglab/imdbclone/engagement"))) {
      assertThat(engagementSources.filter(Files::isRegularFile).map(this::readString))
          .noneMatch(source -> source.contains("account.internal.persistence"));
    }
  }

  @Test
  void accountValidationBelongsToAccountModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.api.AvailableEmail")).isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.api.AvailableUsername"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.validation.AvailableEmail")).isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.validation.AvailableUsername")).isEmpty();
  }

  @Test
  void engagementBelongsToEngagementModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.web.CommentController"))
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
    assertThat(
            classIfPresent("com.thecodinglab.imdbclone.engagement.web.AccountEngagementController"))
        .isEmpty();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.web.AccountEngagementController"))
        .isPresent();
  }

  @Test
  void engagementDoesNotUseCatalogPersistence() throws Exception {
    try (Stream<Path> engagementSources =
        Files.walk(Path.of("src/main/java/com/thecodinglab/imdbclone/engagement"))) {
      assertThat(engagementSources.filter(Files::isRegularFile).map(this::readString))
          .noneMatch(source -> source.contains("catalog.internal.persistence"));
    }
  }

  @Test
  void fileStorageBelongsToMediaModule() {
    assertThat(classIfPresent("com.thecodinglab.imdbclone.media.web.FileStorageController"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.media.internal.MediaServiceImpl"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.media.internal.MinioClientConfig"))
        .isPresent();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/controller/FileStorageController.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/FileStorageService.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/impl/FileStorageServiceImpl.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/validation/ImageSize.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/config/MinioClientConfig.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/utility/images/Image.java")))
        .isFalse();
  }

  @Test
  void emailNotificationsBelongToNotificationModule() {
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.notification.internal.EmailNotificationService"))
        .isPresent();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/service/EmailService.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/thecodinglab/imdbclone/service/impl/EmailServiceImpl.java")))
        .isFalse();
  }

  @Test
  void openTriviaClientBelongsToRecommendationModule() {
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.recommendation.internal.trivia.OpenTriviaService"))
        .isPresent();
    assertThat(
            classIfPresent(
                "com.thecodinglab.imdbclone.recommendation.internal.trivia.OpenTriviaClientConfig"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.rest.OpenTriviaService")).isEmpty();
    assertThat(
            Files.exists(
                Path.of("src/main/java/com/thecodinglab/imdbclone/config/RestClientConfig.java")))
        .isFalse();
    assertThat(Files.exists(Path.of("src/main/java/com/thecodinglab/imdbclone/rest"))).isFalse();
  }

  @Test
  void integrationTestsAreOrganizedByModule() {
    assertThat(Files.exists(Path.of("src/test/java/com/thecodinglab/imdbclone/support"))).isTrue();
    assertThat(Files.exists(Path.of("src/test/java/com/thecodinglab/imdbclone/integration")))
        .isFalse();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.AccountControllerTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.account.AccountRepositoryTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.catalog.MovieControllerTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.catalog.SearchControllerTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.CommentControllerTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.RatingControllerTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.engagement.WatchedMovieControllerTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.identity.AuthenticationControllerTest"))
        .isPresent();
    assertThat(classIfPresent("com.thecodinglab.imdbclone.shared.DatabaseSchemaTest")).isPresent();
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

  private String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not read " + path, ex);
    }
  }
}
