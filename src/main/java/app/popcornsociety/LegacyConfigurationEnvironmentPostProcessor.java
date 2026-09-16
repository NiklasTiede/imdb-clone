package app.popcornsociety;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/** Keeps existing deployment settings usable until their separate infrastructure migration. */
public final class LegacyConfigurationEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {
  private static final String ALIAS_SUFFIX = "-popcorn-compatibility";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    var sources = environment.getPropertySources();
    for (PropertySource<?> source : sources.stream().toList()) {
      if (!(source instanceof EnumerablePropertySource<?> enumerable)
          || source.getName().endsWith(ALIAS_SUFFIX)) {
        continue;
      }
      Map<String, Object> aliases = new LinkedHashMap<>();
      for (String name : enumerable.getPropertyNames()) {
        if (name.startsWith("imdb-clone.")) {
          aliases.put(
              "popcorn-society." + name.substring("imdb-clone.".length()),
              source.getProperty(name));
        } else if (name.startsWith("IMDB_CLONE_")) {
          aliases.put(
              "POPCORN_SOCIETY_" + name.substring("IMDB_CLONE_".length()),
              source.getProperty(name));
        }
      }
      if (!aliases.isEmpty()) {
        String aliasName = source.getName() + ALIAS_SUFFIX;
        // Preserve source precedence; an explicit new setting in the same source wins.
        sources.addAfter(
            source.getName(),
            source instanceof SystemEnvironmentPropertySource
                ? new SystemEnvironmentPropertySource(aliasName, aliases)
                : new MapPropertySource(aliasName, aliases));
      }
    }
  }

  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 1;
  }
}
