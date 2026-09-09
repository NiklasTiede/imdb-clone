package com.thecodinglab.imdbclone.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("architecture")
class BackendArchitectureRulesTest {
  private static final BackendArchitectureRules RULES =
      new BackendArchitectureRules("architecturefixtures");

  @ParameterizedTest
  @MethodSource("violations")
  void rejectsCompiledViolations(ArchRule rule, Class<?> fixture) {
    var result = rule.evaluate(new ClassFileImporter().importClasses(fixture));
    assertThat(result.hasViolation()).as(rule.getDescription()).isTrue();
    assertThat(result.getFailureReport().toString()).contains(fixture.getName());
  }

  static Stream<Arguments> violations() throws ClassNotFoundException {
    return Stream.of(
        Arguments.of(
            RULES.moduleInternals(),
            architecturefixtures.account.internal.IllegalForeignAccess.class),
        Arguments.of(RULES.publicContracts(), architecturefixtures.catalog.api.LeakyApi.class),
        Arguments.of(
            RULES.webPersistence(), architecturefixtures.account.web.RepositoryController.class),
        Arguments.of(
            RULES.sharedIndependence(), architecturefixtures.shared.internal.DomainCoupled.class),
        Arguments.of(
            RULES.identityNotificationSeparation(),
            architecturefixtures.identity.internal.MailCoupled.class),
        Arguments.of(
            RULES.persistenceEncapsulation(), architecturefixtures.catalog.api.ExposedEntity.class),
        Arguments.of(RULES.internalExports(), architecturefixtures.catalog.internal.Export.class),
        Arguments.of(
            RULES.internalExports(),
            Class.forName("architecturefixtures.catalog.internal.exports.package-info")),
        Arguments.of(
            RULES.durableListeners(),
            architecturefixtures.notification.internal.VolatileListener.class),
        Arguments.of(
            RULES.durableListeners(),
            architecturefixtures.notification.internal.MetaVolatileListener.class),
        Arguments.of(
            RULES.domainNames(), architecturefixtures.account.internal.AccountServiceImpl.class));
  }

  @Test
  void acceptsPublicContractsAndTechnicalSharedTypes() {
    var classes =
        new ClassFileImporter()
            .importClasses(
                architecturefixtures.catalog.api.GoodApi.class,
                architecturefixtures.account.internal.CompliantAdapter.class,
                architecturefixtures.account.web.ApiOnlyController.class,
                architecturefixtures.shared.internal.TechnicalUtility.class);
    RULES.all().forEach(rule -> rule.check(classes));
  }
}
