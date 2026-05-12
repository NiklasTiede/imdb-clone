# Spring Modulith Review

## Scope

Use this mode for Spring Modulith readiness now and module-boundary verification after Modulith is introduced.

Primary files:

- `build.gradle`
- `src/main/java/com/thecodinglab/imdbclone`
- future `package-info.java` files with Modulith annotations
- future Modulith tests and generated docs

## Current Baseline to Check

Before Modulith exists, report readiness gaps:

- backend packages are currently technical layers (`controller`, `service`, `repository`, `entity`, `payload`)
- future Modulith modules should become domain/business packages under the application root
- package moves should be planned around domain language, not around framework types

Do not mark absence of Modulith as a finding unless the user asked for Modulith readiness.

## Official Concepts to Apply

Spring Modulith discovers application modules from direct sub-packages of the application package. It verifies module structure with `ApplicationModules.of(Application.class).verify()`.

Important concepts:

- application modules should represent business capabilities
- module internals should not be accessed directly by other modules
- named interfaces expose selected packages or types with `@NamedInterface`
- `@ApplicationModule(allowedDependencies = ...)` can restrict dependencies
- verification rejects cycles, illegal internal access, and disallowed dependencies
- module integration tests can use `@ApplicationModuleTest`
- documentation can be generated from the discovered module model

If syntax or version details matter, consult current Spring Modulith documentation before making exact recommendations.

## Candidate Modules for This Repo

Use these only as hypotheses until the project has an explicit module plan:

- `account`: account profile, public profile, registration identity, roles
- `auth`: login, JWT, registration, password reset, verification tokens
- `catalog`: movies, movie details, catalog reads/writes
- `search`: Elasticsearch-backed search and projection synchronization
- `rating`: user ratings and rating aggregates
- `watchlist`: watched/favorite movies per account
- `comment`: movie comments
- `media`: MinIO image storage and image token handling

Watch for accidental modules that are too technical, such as `repository` or `payload`.

## Boundary Checks

For each proposed or existing module:

- module has a small public API and hides persistence details
- module does not expose JPA entities to unrelated modules unless that is an explicit decision
- controllers call application/service APIs, not repositories from other modules
- repositories stay inside their owning module
- events carry stable IDs or value data, not mutable entity graphs
- named interfaces expose only what other modules need
- dependencies flow through allowed module APIs
- no cyclic dependencies between modules

## Verification Recommendations

Suggest adding deterministic checks when Modulith is introduced:

```java
class ModulithArchitectureTest {

  @Test
  void verifiesApplicationModules() {
    ApplicationModules.of(Application.class).verify();
  }
}
```

Suggest module tests only after modules have a clear API and meaningful behavior to test.
