package com.thecodinglab.imdbclone.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.function.BiPredicate;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.NamedInterface;
import org.springframework.scheduling.annotation.Async;

/** The same semantic rules run against production bytecode and deliberately invalid fixtures. */
public final class BackendArchitectureRules {
  private final String root;

  public BackendArchitectureRules(String root) {
    this.root = root;
  }

  public ArchRule moduleInternals() {
    return dependencies(
        "access another module only through its public contracts",
        (source, target) ->
            !module(source).isEmpty()
                && !module(target).isEmpty()
                && !module(source).equals(module(target))
                && internal(target));
  }

  public ArchRule publicContracts() {
    return dependencies(
        "keep public contracts independent of implementation and web adapters",
        (source, target) -> api(source) && (internal(target) || web(target)));
  }

  public ArchRule webPersistence() {
    return dependencies(
        "keep persistence and external storage behind application services",
        (source, target) ->
            web(source)
                && (target.isAssignableTo(org.springframework.data.repository.Repository.class)
                    || target.getName().equals("org.springframework.jdbc.core.JdbcTemplate")
                    || target.getName().equals("jakarta.persistence.EntityManager")
                    || target.isAnnotatedWith(jakarta.persistence.Entity.class)
                    || target.getPackageName().startsWith("software.amazon.awssdk.services.s3")
                    || target.getPackageName().startsWith("org.opensearch.client")));
  }

  public ArchRule sharedIndependence() {
    return dependencies(
        "keep shared independent of business modules",
        (source, target) ->
            module(source).equals("shared")
                && !module(target).isEmpty()
                && !module(target).equals("shared"));
  }

  public ArchRule identityNotificationSeparation() {
    return dependencies(
        "keep Identity independent of Notification and mail/template adapters",
        (source, target) ->
            module(source).equals("identity")
                && (module(target).equals("notification")
                    || target.getPackageName().startsWith("org.springframework.mail")
                    || target.getPackageName().startsWith("jakarta.mail")
                    || target.getPackageName().startsWith("org.thymeleaf")));
  }

  public ArchRule persistenceEncapsulation() {
    return classesInRoot()
        .should(
            new ArchCondition<>("keep entities and repositories internal") {
              @Override
              public void check(JavaClass type, ConditionEvents events) {
                boolean persistence =
                    type.isAnnotatedWith(jakarta.persistence.Entity.class)
                        || type.isAssignableTo(
                            org.springframework.data.repository.Repository.class);
                reject(
                    events,
                    type,
                    persistence && !internal(type),
                    "Persistence type must be internal: " + type.getName());
              }
            });
  }

  public ArchRule internalExports() {
    return classesInRoot()
        .should(
            new ArchCondition<>("not export internal types or packages as named interfaces") {
              @Override
              public void check(JavaClass type, ConditionEvents events) {
                reject(
                    events,
                    type,
                    internal(type) && type.isAnnotatedWith(NamedInterface.class),
                    "Internal type/package must not be a NamedInterface: " + type.getName());
              }
            });
  }

  public ArchRule durableListeners() {
    return classesInRoot()
        .should(
            new ArchCondition<>("not dispatch domain events through volatile async listeners") {
              @Override
              public void check(JavaClass type, ConditionEvents events) {
                for (var method : type.getAllMethods()) {
                  boolean listener =
                      method.isAnnotatedWith(EventListener.class)
                          || method.isMetaAnnotatedWith(EventListener.class);
                  boolean async =
                      type.isAnnotatedWith(Async.class)
                          || type.isMetaAnnotatedWith(Async.class)
                          || method.isAnnotatedWith(Async.class)
                          || method.isMetaAnnotatedWith(Async.class);
                  reject(
                      events,
                      method,
                      listener && async,
                      "Event listener must persist work before asynchronous dispatch: "
                          + method.getFullName());
                }
              }
            });
  }

  public ArchRule domainNames() {
    return classesInRoot()
        .should(
            new ArchCondition<>("use domain names for concrete implementations") {
              @Override
              public void check(JavaClass type, ConditionEvents events) {
                reject(
                    events,
                    type,
                    type.getSimpleName().endsWith("ServiceImpl"),
                    "Implementation must have a domain name: " + type.getName());
              }
            });
  }

  public List<ArchRule> all() {
    return List.of(
        moduleInternals(),
        publicContracts(),
        webPersistence(),
        sharedIndependence(),
        identityNotificationSeparation(),
        persistenceEncapsulation(),
        internalExports(),
        durableListeners(),
        domainNames());
  }

  private ArchRule dependencies(String description, BiPredicate<JavaClass, JavaClass> forbidden) {
    return classesInRoot()
        .should(
            new ArchCondition<>(description) {
              @Override
              public void check(JavaClass source, ConditionEvents events) {
                for (var dependency : source.getDirectDependenciesFromSelf()) {
                  reject(
                      events,
                      dependency,
                      forbidden.test(source, dependency.getTargetClass()),
                      dependency.getDescription());
                }
              }
            });
  }

  private com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction classesInRoot() {
    return classes().that().resideInAPackage(root + "..");
  }

  private String module(JavaClass type) {
    String prefix = root + ".";
    if (!type.getPackageName().startsWith(prefix)) {
      return "";
    }
    return type.getPackageName().substring(prefix.length()).split("\\.", 2)[0];
  }

  private boolean internal(JavaClass type) {
    return inLayer(type, "internal");
  }

  private boolean api(JavaClass type) {
    return inLayer(type, "api");
  }

  private boolean web(JavaClass type) {
    return inLayer(type, "web");
  }

  private boolean inLayer(JavaClass type, String layer) {
    String name = type.getPackageName();
    String base = root + "." + module(type) + "." + layer;
    return name.equals(base) || name.startsWith(base + ".");
  }

  private static void reject(
      ConditionEvents events, Object object, boolean invalid, String message) {
    if (invalid) {
      events.add(SimpleConditionEvent.violated(object, message));
    }
  }
}
