package com.thecodinglab.imdbclone;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

  private static final Path MODULE_ROOT = Path.of("src/main/java/com/thecodinglab/imdbclone");

  private static final Set<String> EXPECTED_MODULES =
      Set.of(
          "account",
          "catalog",
          "engagement",
          "identity",
          "media",
          "notification",
          "recommendation",
          "shared");

  private static final Pattern INTERNAL_IMPORT =
      Pattern.compile(
          "^import\\s+com\\.thecodinglab\\.imdbclone\\.([a-z]+)\\.internal\\.", Pattern.MULTILINE);

  @Test
  void verifiesApplicationModules() {
    ApplicationModules.of(Application.class).verify();
  }

  @Test
  void detectsExpectedApplicationModules() {
    ApplicationModules modules = ApplicationModules.of(Application.class);

    assertThat(modules.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
        .containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
  }

  @Test
  void applicationModulesDeclareAllowedDependencies() {
    assertThat(EXPECTED_MODULES)
        .allSatisfy(
            module ->
                assertThat(readString(MODULE_ROOT.resolve(module).resolve("package-info.java")))
                    .as(module)
                    .contains("allowedDependencies"));
  }

  @Test
  void apiPackagesAreNamedInterfaces() throws IOException {
    try (Stream<Path> packageInfos = Files.walk(MODULE_ROOT)) {
      assertThat(
              packageInfos
                  .filter(path -> path.endsWith("api/package-info.java"))
                  .filter(path -> !readString(path).contains("@NamedInterface(\"api\")"))
                  .toList())
          .isEmpty();
    }
  }

  @Test
  void internalPackagesAreNotNamedInterfaces() throws IOException {
    try (Stream<Path> packageInfos = Files.walk(MODULE_ROOT)) {
      assertThat(
              packageInfos
                  .filter(path -> path.getFileName().toString().equals("package-info.java"))
                  .filter(path -> path.toString().contains("/internal/"))
                  .filter(path -> readString(path).contains("@NamedInterface"))
                  .toList())
          .isEmpty();
    }
  }

  @Test
  void registrationRequestBelongsToIdentityModule() {
    assertThat(Files.exists(MODULE_ROOT.resolve("identity/api/RegistrationRequest.java"))).isTrue();
    assertThat(Files.exists(MODULE_ROOT.resolve("account/api/RegistrationRequest.java"))).isFalse();
  }

  @Test
  void catalogConsumersUseNarrowNamedInterfaces() {
    assertThat(readString(MODULE_ROOT.resolve("engagement/package-info.java")))
        .contains("catalog::reference", "catalog::ratings")
        .doesNotContain("catalog::api");
    assertThat(readString(MODULE_ROOT.resolve("media/package-info.java")))
        .contains("catalog::media")
        .doesNotContain("catalog::api");
    assertThat(readString(MODULE_ROOT.resolve("recommendation/package-info.java")))
        .contains("catalog::recommendation")
        .doesNotContain("catalog::api");
  }

  @Test
  void identityPublishesNotificationEventsInsteadOfCallingNotificationModule() throws IOException {
    assertThat(readString(MODULE_ROOT.resolve("identity/package-info.java")))
        .doesNotContain("notification");
    assertThat(readString(MODULE_ROOT.resolve("identity/api/events/package-info.java")))
        .contains("@NamedInterface(\"events\")");
    assertThat(readString(MODULE_ROOT.resolve("notification/package-info.java")))
        .contains("identity::events")
        .doesNotContain("identity::api");
    assertThat(Files.exists(MODULE_ROOT.resolve("notification/api/NotificationService.java")))
        .isFalse();
    try (Stream<Path> identitySources = Files.walk(MODULE_ROOT.resolve("identity"))) {
      assertThat(
              identitySources
                  .filter(Files::isRegularFile)
                  .map(this::readString)
                  .filter(
                      source ->
                          source.contains("com.thecodinglab.imdbclone.notification")
                              || source.contains("NotificationService")
                              || source.contains("buildConfirmationEmail")
                              || source.contains("buildPasswordResetEmail")
                              || source.contains("sendEmail("))
                  .toList())
          .isEmpty();
    }
  }

  @Test
  void mediaDoesNotExposeUploadInterfaceAsModuleApi() {
    assertThat(Files.exists(MODULE_ROOT.resolve("media/api/MediaService.java"))).isFalse();
  }

  @Test
  void accountUsesSingleEngagementProfileInterface() throws IOException {
    assertThat(readString(MODULE_ROOT.resolve("account/package-info.java")))
        .contains("engagement::profile")
        .doesNotContain("engagement::api");
    try (Stream<Path> accountSources = Files.walk(MODULE_ROOT.resolve("account"))) {
      assertThat(
              accountSources
                  .filter(Files::isRegularFile)
                  .map(this::readString)
                  .filter(
                      source ->
                          source.contains("CommentService")
                              || source.contains("RatingService")
                              || source.contains("WatchedMovieService")
                              || source.contains("EngagementStatsService"))
                  .toList())
          .isEmpty();
    }
  }

  @Test
  void concreteImplementationsUseDomainNames() throws IOException {
    assertThat(
            javaSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("ServiceImpl.java"))
                .toList())
        .isEmpty();
  }

  @Test
  void apiPackagesDoNotDependOnModuleInternals() throws IOException {
    assertThat(
            javaSources().stream()
                .filter(path -> path.toString().contains("/api/"))
                .filter(path -> referencesOwnInternalPackage(path))
                .toList())
        .isEmpty();
  }

  @Test
  void modulesDoNotImportOtherModulesInternalPackages() throws IOException {
    assertThat(
            javaSources().stream()
                .flatMap(
                    source ->
                        importedInternalModules(source)
                            .filter(targetModule -> !targetModule.equals(moduleName(source)))
                            .map(targetModule -> source + " imports " + targetModule + ".internal"))
                .toList())
        .isEmpty();
  }

  private List<Path> javaSources() throws IOException {
    try (Stream<Path> sources = Files.walk(MODULE_ROOT)) {
      return sources
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .toList();
    }
  }

  private boolean referencesOwnInternalPackage(Path source) {
    String moduleName = moduleName(source);

    return EXPECTED_MODULES.contains(moduleName)
        && readString(source).contains("com.thecodinglab.imdbclone." + moduleName + ".internal.");
  }

  private Stream<String> importedInternalModules(Path source) {
    return INTERNAL_IMPORT.matcher(readString(source)).results().map(result -> result.group(1));
  }

  private String moduleName(Path source) {
    Path relativePath = MODULE_ROOT.relativize(source);

    return relativePath.getNameCount() > 1 ? relativePath.getName(0).toString() : "";
  }

  private String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not read " + path, ex);
    }
  }
}
