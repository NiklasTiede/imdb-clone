# Spring Modulith Review

## Scope

Use this mode for the current Spring Modulith architecture: application modules, named interfaces, allowed dependencies, and hard architecture checks.

Primary files:

- `build.gradle`
- `src/main/resources/config/application.properties`
- `src/main/java/com/thecodinglab/imdbclone`
- module `package-info.java` files with `@ApplicationModule`
- named interface `package-info.java` files and type-level `@NamedInterface`
- `src/test/java/com/thecodinglab/imdbclone/ModulithArchitectureTest.java`

## Current Baseline

This repository has introduced a clean Modulith baseline. Treat these as the expected backend application modules unless the implementation or an ADR says otherwise:

- `account`
- `catalog`
- `engagement`
- `identity`
- `media`
- `notification`
- `recommendation`
- `shared`

The app uses explicit module detection. Do not treat unannotated direct subpackages as implicit modules unless the configuration changes.

Package roles:

- `api`: module public API and named interfaces
- `web`: REST controllers and request entrypoints
- `internal`: module implementation, persistence, security, infrastructure, schedulers, mappers, and search adapters
- `shared`: shared kernel only; keep it small and stable

## Official Concepts to Apply

Spring Modulith can discover application modules from direct sub-packages of the application package, or from explicitly annotated packages when configured that way. This repo should be verified with `ApplicationModules.of(Application.class).verify()`.

Important concepts:

- application modules should represent business capabilities
- module internals should not be accessed directly by other modules
- named interfaces expose selected packages or types with `@NamedInterface`
- `@ApplicationModule(allowedDependencies = ...)` can restrict dependencies
- verification rejects cycles, illegal internal access, and disallowed dependencies
- module integration tests can use `@ApplicationModuleTest`
- documentation can be generated from the discovered module model

If syntax or version details matter, consult current Spring Modulith documentation before making exact recommendations.

## Module Dependency Expectations

Use actual `package-info.java` files as the source of truth, then compare them with these intended dependency shapes:

- `account` may use engagement profile APIs plus shared kernel interfaces.
- `catalog` should not depend on other business modules.
- `engagement` may use narrow catalog reference and ratings interfaces.
- `identity` may use account APIs and should publish notification events instead of calling notification internals.
- `media` may use account APIs and narrow catalog media interfaces.
- `notification` may consume identity events, not identity internals.
- `recommendation` should stay isolated until it has an explicit integration.
- `shared` should not depend on business modules.

Watch for broad dependencies where a narrow named interface already exists, such as `catalog::api` instead of `catalog::reference`, `catalog::ratings`, or `catalog::media`.

## Boundary Checks

For each module:

- module has a small public API and hides internal details
- module does not expose JPA entities to unrelated modules unless that is an explicit decision
- controllers stay in `web` and call owning module APIs or internal application services
- repositories, entities, mappers, infrastructure adapters, and schedulers stay in `internal`
- events carry stable IDs or value data, not mutable entity graphs
- named interfaces expose only what other modules need
- dependencies flow through allowed module APIs
- no cyclic dependencies between modules
- no module imports another module's `internal` package
- `api` packages do not depend on their own `internal` package
- concrete implementations use domain names, not generic `*ServiceImpl` names
- new cross-module dependencies update `allowedDependencies` deliberately and narrowly

## Verification Recommendations

Prefer the existing hard check:

```bash
./gradlew test --tests "com.thecodinglab.imdbclone.ModulithArchitectureTest"
```

It should verify the discovered modules, allowed dependency declarations, named-interface conventions, and selected repo-specific dependency rules.

The core Modulith verification pattern is:

```java
class ModulithArchitectureTest {

  @Test
  void verifiesApplicationModules() {
    ApplicationModules.of(Application.class).verify();
  }
}
```

Suggest `@ApplicationModuleTest` only when a module has a clear public API and behavior worth testing in isolation. Do not replace module-boundary tests with broader integration tests.
