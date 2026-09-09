package com.thecodinglab.imdbclone;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.architecture.BackendArchitectureRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.modulith.NamedInterface;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

@Tag("architecture")
class ModulithArchitectureTest {
  private static final String ROOT = "com.thecodinglab.imdbclone";
  private static final Set<String> EXPECTED_MODULES =
      Set.of(
          "account",
          "assistant",
          "catalog",
          "engagement",
          "identity",
          "media",
          "notification",
          "recommendation",
          "shared");
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  @Test
  void verifiesApplicationModules() {
    ApplicationModules.of(Application.class).verify();
  }

  @Test
  void detectsExpectedApplicationModules() {
    assertThat(
            ApplicationModules.of(Application.class).stream()
                .map(ApplicationModule::getIdentifier)
                .map(Object::toString))
        .containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
    assertThat(CLASSES).isNotEmpty();
  }

  @TestFactory
  Stream<DynamicTest> semanticArchitectureRules() {
    return new BackendArchitectureRules(ROOT)
        .all().stream()
            .map(rule -> DynamicTest.dynamicTest(rule.getDescription(), () -> rule.check(CLASSES)));
  }

  @Test
  void applicationModulesDeclareClosedDependencies() throws Exception {
    String[] unrestricted =
        (String[])
            org.springframework.modulith.ApplicationModule.class
                .getMethod("allowedDependencies")
                .getDefaultValue();
    for (String module : EXPECTED_MODULES) {
      assertThat(dependencies(module))
          .as(module)
          .doesNotContainAnyElementsOf(Arrays.asList(unrestricted));
    }
  }

  @Test
  void apiPackagesAreNamedInterfaces() throws Exception {
    var packages =
        CLASSES.stream()
            .map(type -> type.getPackageName())
            .filter(name -> name.endsWith(".api"))
            .distinct()
            .toList();
    assertThat(packages).isNotEmpty();
    for (String name : packages) {
      var annotation =
          Class.forName(name + ".package-info").getPackage().getAnnotation(NamedInterface.class);
      assertThat(annotation).as(name).isNotNull();
      assertThat(annotation.value()).as(name).contains("api");
    }
  }

  @Test
  void consumersUseNarrowNamedInterfaces() throws Exception {
    assertThat(dependencies("assistant"))
        .contains("catalog::assistant")
        .doesNotContain("catalog::api");
    assertThat(dependencies("engagement"))
        .contains("catalog::reference", "catalog::ratings")
        .doesNotContain("catalog::api");
    assertThat(dependencies("media")).contains("catalog::media").doesNotContain("catalog::api");
    assertThat(dependencies("recommendation"))
        .contains("catalog::recommendation")
        .doesNotContain("catalog::api");
    assertThat(dependencies("account"))
        .contains("engagement::profile", "engagement::lifecycle")
        .doesNotContain("engagement::api");
    assertThat(dependencies("notification")).containsExactly("identity::events");
    assertThat(dependencies("media")).contains("account::media").doesNotContain("account::api");
    assertThat(dependencies("identity"))
        .contains("account::identity")
        .doesNotContain("account::api");
    assertThat(dependencies("identity"))
        .noneMatch(dependency -> dependency.startsWith("notification"));
  }

  @Test
  void registrationAndUploadContractsKeepTheirOwnership() {
    assertThat(
            CLASSES.stream()
                .filter(type -> type.getSimpleName().equals("RegistrationRequest"))
                .map(type -> type.getPackageName()))
        .containsExactly(ROOT + ".identity.api");
    assertThat(
            CLASSES.stream()
                .filter(type -> type.getSimpleName().equals("MediaService"))
                .map(type -> type.getPackageName()))
        .containsExactly(ROOT + ".media.internal");
  }

  private String[] dependencies(String module) throws ClassNotFoundException {
    var annotation =
        Class.forName(ROOT + "." + module + ".package-info")
            .getPackage()
            .getAnnotation(org.springframework.modulith.ApplicationModule.class);
    assertThat(annotation).as(module).isNotNull();
    return annotation.allowedDependencies();
  }
}
