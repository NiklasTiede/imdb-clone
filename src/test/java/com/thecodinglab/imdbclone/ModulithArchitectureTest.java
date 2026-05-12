package com.thecodinglab.imdbclone;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieService;
import java.lang.reflect.Method;
import java.util.Arrays;
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

  private Stream<Class<?>> methodTypes(Method method) {
    return Stream.concat(
        Stream.of(method.getReturnType()), Arrays.stream(method.getParameterTypes()));
  }
}
