package com.thecodinglab.imdbclone;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieService;
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
  void catalogApiDoesNotExposeInternalTypes() {
    assertThat(Arrays.stream(MovieService.class.getMethods()).flatMap(this::methodTypes))
        .extracting(Class::getName)
        .noneMatch(typeName -> typeName.contains(".catalog.internal."));
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

  private Stream<Class<?>> methodTypes(Method method) {
    return Stream.concat(
        Stream.of(method.getReturnType()), Arrays.stream(method.getParameterTypes()));
  }

  private Optional<Class<?>> classIfPresent(String className) {
    try {
      return Optional.of(Class.forName(className));
    } catch (ClassNotFoundException ex) {
      return Optional.empty();
    }
  }
}
