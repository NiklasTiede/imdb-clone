# Spring Modulith Catalog Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Introduce Spring Modulith and migrate the movie catalog code into the first business module.

**Architecture:** Use Spring Modulith's explicitly annotated module detection so the project can migrate incrementally. The first module is `catalog`, which owns movie metadata, catalog REST endpoints, movie persistence, and movie DTOs. Existing technical packages remain in place until later slices migrate them.

**Tech Stack:** Java 25, Spring Boot 4.0.6, Spring Modulith, Gradle, JUnit 5.

---

### Task 1: Add Modulith Verification

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/config/application.properties`
- Create: `src/test/java/com/thecodinglab/imdbclone/ModulithArchitectureTest.java`

- [x] **Step 1: Write the architecture test**

Create a JUnit test that calls `ApplicationModules.of(Application.class).verify()`.

- [x] **Step 2: Add Modulith dependencies**

Add Spring Modulith core and test starter dependencies managed by Spring Boot's dependency management.

- [x] **Step 3: Enable incremental detection**

Set `spring.modulith.detection-strategy=explicitly-annotated` so only annotated modules are verified during migration.

- [x] **Step 4: Run the focused test**

Run: `./gradlew test --tests "com.thecodinglab.imdbclone.ModulithArchitectureTest"`

Expected before catalog metadata exists: failure because no module is annotated or no useful module model exists.

### Task 2: Create the Catalog Module

**Files:**
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/package-info.java`
- Move: movie controller, movie service, movie persistence, movie mapper, and movie DTO classes into `catalog` subpackages.

- [x] **Step 1: Add `@ApplicationModule` metadata**

Create `catalog/package-info.java` with module id `catalog`.

- [x] **Step 2: Move catalog API types**

Move movie DTOs and the `MovieService` interface to `com.thecodinglab.imdbclone.catalog.api`.

- [x] **Step 3: Move catalog implementation types**

Move the movie controller, service implementation, mapper, entity, and repositories under `com.thecodinglab.imdbclone.catalog`.

- [x] **Step 4: Update imports**

Update all references to moved catalog classes.

### Task 3: Verify and Clean Up

**Files:**
- All moved Java files
- Any tests with moved imports

- [x] **Step 1: Run catalog architecture verification**

Run: `./gradlew test --tests "com.thecodinglab.imdbclone.ModulithArchitectureTest"`

Expected: pass.

- [x] **Step 2: Run focused backend tests**

Run: `./gradlew test --tests "com.thecodinglab.imdbclone.integration.controller.MovieControllerTest"`

Expected: pass.

- [x] **Step 3: Run broader backend tests if the focused test passes**

Run: `./gradlew test`

Expected: pass, or report unrelated environmental failures with exact output.

---

### Transitional Architecture Debt

The catalog migration is intentionally incremental. `catalog` is the first explicitly annotated
Spring Modulith module, while the rest of the backend still uses the legacy package layout.

Known follow-up work:

- Move movie search/indexing behind the catalog module or introduce a dedicated search module with
  a catalog-owned adapter. The current Elasticsearch service still exposes catalog implementation
  types.
- Migrate comments, ratings, and watched movies into their own modules. Their JPA entities currently
  reference `catalog.internal.persistence.Movie`; a cleaner module shape should use `movieId` across
  module seams and query catalog through its public API when movie details are needed.
- Keep `catalog.api` free of persistence, mapper, Elasticsearch, and security principal types.
