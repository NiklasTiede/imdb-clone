# Left-Shift Engineering Roadmap

This roadmap records opportunities to make defects, contract drift, and architecture violations fail
as early as practical. The main consumer is a coding agent: fast, deterministic feedback lets the
agent correct its own work before a human review or runtime deployment.

The working principle is:

> Move failures from production to tests, from tests to the build, and from the build into static
> analysis and type checking. Keep runtime and persistence checks as defense in depth.

## How We Evaluate Changes

Adopt one improvement at a time:

1. Record a repeatable baseline, preferably the median of several warm runs.
2. Enable one rule, test lane, or verification tool.
3. Review every new finding instead of suppressing the category broadly.
4. Measure feedback time, false positives, flakiness, and maintenance cost.
5. Keep the change only when the earlier feedback is worth its cost.

Fast checks should be available to agents after each edit. Full integration, mutation, browser, and
container checks remain mandatory at the appropriate handoff or CI gate.

## Current Baseline

Backend observations from 2026-07-11:

- Java 25 toolchain, Spring Modulith verification, Spotless, JaCoCo, and Testcontainers are present.
- A forced compile and full test run completed 180 tests in about 63 seconds.
- Nineteen Spring/Testcontainers classes accounted for about 44 seconds of summed suite time; all
  other backend classes accounted for about 5 seconds.
- Gradle uses one test JVM by default. Global parallel forks are unsafe while integration tests share
  singleton containers and destructive SQL fixtures.
- PostgreSQL, OpenSearch, and RustFS start sequentially. OpenSearch dominates their startup time.

Frontend observations from 2026-07-11:

- TypeScript strict mode, `tsc --noEmit`, generated OpenAPI types, ESLint, Zod, Vitest, and Playwright
  are present.
- Vitest completed 254 tests across 89 files in about 21 seconds with its default parallel workers.
- Forcing two or four workers was slower. A thread-pool run was about 10 percent faster in one trial
  and needs repeated benchmarking before adoption.
- Every test currently pays for a jsdom environment, including pure model and utility tests.
- Passing React tests emit unexpected `act(...)` warnings, which weakens the signal available to an
  agent.

## Backend Opportunities

| Status | Experiment | Expected early feedback |
| --- | --- | --- |
| Adopted | Split fast and integration test execution | Run pure module and architecture tests without Docker; retain all tests under `check` and `build`. |
| Adopted | Enable high-signal `javac -Xlint` checks with `-Werror` | Reject unchecked, deprecated, and suspicious Java constructs during compilation. |
| Adopted | Add Error Prone | Catch semantic Java mistakes during compilation; the Java 25 adoption trial produced 31 diagnostics without requiring a globally disabled check. |
| Adopted | Add JSpecify and NullAway incrementally | Make nullability part of deterministic package interfaces; the first three checked packages use non-null defaults with explicit nullable exceptions. |
| Adopted | Introduce high-value domain types incrementally | Make invalid values unconstructable where one type can concentrate a real invariant; the first type is `RatingScore`. |
| Adopted | Model expected outcomes with sealed types where callers branch | Use closed state models when variants own different data; reindex jobs now map states through a compiler-exhaustive switch. |
| Adopted | Add property-based tests for pure invariants | Exercise broad deterministic input spaces where shrinking produces useful examples; rank fusion is the first target. |
| Declined for now | Add targeted mutation testing | The focused PIT trial could not execute the JUnit 6 suite with the latest stable adapter; avoid a legacy shadow suite and revisit when the adapter supports this test platform. |
| Adopted | Add risk-based JaCoCo branch thresholds | Preserve complete branch coverage in compact, high-risk pure logic without imposing a misleading repository-wide percentage. |
| Declined | Start independent Testcontainers concurrently | A cached-image trial changed a 35-second targeted integration run into a hang beyond four minutes even though all services became healthy; keep deterministic sequential startup. |
| Candidate | Consolidate avoidable Spring test contexts | Reduce repeated application-context and connection-pool startup. |
| Candidate | Parallelize only the fast backend lane | Use measured Gradle forks without duplicating containers or Spring context caches. |
| Adopted | Enable dependency locking and verification | Pin every resolved backend dependency and reject artifacts whose SHA-256 checksum is not reviewed metadata. |
| Candidate | Replace source-text architecture checks where practical | Verify module dependencies and seams semantically rather than through formatting-sensitive scans. |

## Frontend Opportunities

| Status | Experiment | Expected early feedback |
| --- | --- | --- |
| Adopted | Enable `noUncheckedIndexedAccess` | Detect possibly missing array and indexed values; the adoption trial found eleven issues. |
| Adopted | Enable `exactOptionalPropertyTypes` | Distinguish omitted properties from explicit `undefined`; a local generator template keeps generated configuration compatible. |
| Adopted | Type-check Vite, Playwright, and end-to-end files | Bring TypeScript outside `src` into the normal build gate; the adoption trial found three issues. |
| Adopted | Enable type-aware typescript-eslint rules | Catch floating promises, unsafe values, misused promises, and non-exhaustive handling; the curated adoption trial found 39 actionable issues. |
| Adopted | Parse high-risk trust seams with runtime schemas | Validate authenticated session responses and login navigation state before use; expand only where boundary risk justifies a schema. |
| Adopted | Strengthen OpenAPI required and nullable contracts incrementally | Generate useful transport types by documenting guaranteed response fields; the first contract covers authenticated sessions. |
| Candidate | Adapt transport types into feature-owned domain types | Normalize wire arrays/sets and establish required fields at one seam. |
| Candidate | Use discriminated unions for application state | Make impossible authentication, upload, and mutation states unrepresentable. |
| Candidate | Split Node and jsdom Vitest projects | Avoid browser-environment startup for pure logic tests. |
| Candidate | Fail on unexpected console warnings and errors | Prevent React warnings and observability noise from hiding real failures. |
| Candidate | Benchmark the Vitest thread pool repeatedly | Adopt it only if the median improvement is stable and tests remain isolated. |
| Candidate | Add lint, Vitest, and generated-client drift gates to CI | Ensure CI exercises the same frontend verification expected locally. |

## Repository And Agent Harness Opportunities

- Provide one fast command for compilation, static analysis, architecture tests, backend fast tests,
  frontend type checking, and frontend fast tests.
- Provide one full command for integration tests, complete coverage, production builds, and contract
  drift checks.
- Keep `AGENTS.md` short and use it as a map to owned, maintained documentation.
- Write feature acceptance examples and invariants before implementation when behavior is ambiguous.
- Prefer deterministic generators, fixtures, clocks, random seeds, and disposable environments.
- Preserve structured failure artifacts such as test reports, Playwright traces, schema diffs, and
  mutation reports.
- Keep agent permissions broad inside the workspace but require approval for secrets, deployments,
  destructive data operations, and other large blast-radius actions.
- Review agent changes through intent, risks, changed contracts, and verification evidence rather
  than code volume.

## Backend Test Lanes

The first adopted improvement establishes these commands:

| Purpose | Command |
| --- | --- |
| One fast backend test | `./gradlew test --tests "com.thecodinglab.imdbclone.SomeTest"` |
| All fast backend tests | `./gradlew test` |
| One integration test | `./gradlew integrationTest --tests "com.thecodinglab.imdbclone.SomeIntegrationTest"` |
| All integration tests | `./gradlew integrationTest` |
| Full backend verification | `./gradlew check` |
| CI-equivalent build and combined coverage | `./gradlew build jacocoTestReport` |

Tests inheriting from `BaseContainers` inherit the JUnit `integration` tag. Integration tests that do
not inherit from it must declare `@Tag("integration")` directly. The integration lane remains
single-process until its PostgreSQL fixtures and other mutable adapters are isolated.

## Backend Compiler Policy

Every `JavaCompile` task uses UTF-8 and enables all Java 25 lint categories except `processing` and
`serial`. Compiler warnings fail the build through `-Werror`.

The initial diagnostic found one genuine unchecked conversion in a Mockito argument captor. It was
replaced with Mockito's type-inferred `ArgumentCaptor.captor()` interface. The excluded categories
were intentionally rejected:

- `processing` warns that ordinary Spring, Jakarta, and other runtime annotations are not claimed by
  an annotation processor. That is expected and produces a large, unstable warning with no defect
  signal.
- `serial` requires serialization identifiers and transient-field decisions across security
  principals, exceptions, and persistence IDs. Java serialization is not an application storage or
  transport interface here, so the added ceremony would not improve relevant safety.

Revisit `serial` if Java object serialization becomes an intentional compatibility interface. Do not
add broad source-level warning suppressions to bypass the adopted compiler policy.

## Error Prone Policy

Every main and test Java compilation runs Error Prone 2.50.0 through the Gradle Error Prone plugin.
The plugin supplies the Java 25 module exports and isolates the compiler process. Generated-code
warnings are disabled because MapStruct owns that source; checks remain enabled for handwritten main
and test code, and existing `-Werror` policy promotes Error Prone warnings to build failures.

The adoption trial produced 31 diagnostics. Fixes made account and email normalization independent of
the host locale, aligned base and social principal identity, added missing interface overrides,
delegated servlet bulk reads efficiently, made deferred CSRF loading explicit, simplified controller
mappings, and closed a filesystem stream within the module that opens it. Three JPA audit fields are
read reflectively by Hibernate, so only those fields carry a narrow `UnusedVariable` suppression.
Do not disable checks globally to accommodate framework reflection; suppress the smallest justified
interface and explain why.

## Backend Nullability Policy

NullAway 0.13.7 runs as an Error Prone error for main code, and JSpecify supplies source-level
nullability contracts. NullAway checks only packages explicitly marked with `@NullMarked`; do not
assume that unmarked packages have been verified.

The first checked packages are catalog search-query construction and ranking, media image handling,
and shared validation. These deterministic modules have small interfaces and little framework-driven
null behavior. Their unannotated types are non-null by default. Use `@Nullable` where absence is an
intentional part of the interface; Bean Validation inputs are the first example because custom
validators deliberately leave null rejection to `@NotNull`.

Expand package by package when working in a module. Review every finding and document framework
semantics rather than marking the repository root or persistence packages mechanically. A temporary
negative-control compile confirmed that returning null from a method in a marked package fails with a
NullAway error; that deliberate defect was reverted after the check.

## Backend Dependency Reproducibility Policy

Gradle locks every resolvable configuration in strict mode through the checked-in `gradle.lockfile`.
Dependency verification checks artifact and metadata SHA-256 values from
`gradle/verification-metadata.xml` on every build, including build plugins. This makes unexpected
transitive upgrades, missing lock state, and changed repository artifacts fail before compilation.

Update the files intentionally after reviewing a dependency change:

```bash
./gradlew dependencies spotlessApply --write-locks --write-verification-metadata sha256
```

Checksum generation is trust-on-first-use, not independent provenance verification. Review the
requested dependency/version changes before accepting generated metadata, and never regenerate the
file merely to silence a verification failure. Resolving `spotlessApply` is required because its
Google Java Format dependency is provisioned lazily and does not appear in the ordinary dependency
report. The current graph contains 394 lock entries and 5,606 checksum-metadata lines. A fully
offline test passed; a deliberate checksum mismatch failed at project configuration and was reverted
after proving the gate.

## Backend Domain Type Policy

Introduce a domain type when it concentrates a meaningful invariant at a narrow module interface and
removes repeated validation or unsafe primitive operations. Do not wrap primitives mechanically when
the conversions would spread across controllers, generated contracts, persistence, and unrelated
module interfaces.

The first adopted type is `RatingScore`. The engagement web adapter converts the path value once, and
the engagement module accepts only a constructed score. It guarantees the zero-to-ten range and at
most one decimal place, matching the database's `numeric(3,1)` representation. This closes a previous
gap where a value such as `8.55` passed Java validation and could be rounded silently by PostgreSQL.
The persistence adapter still stores `BigDecimal`, keeping JPA mapping simple.

Typed account and movie IDs were evaluated but not introduced: they currently cross too many Spring,
OpenAPI, persistence, and module interfaces for the conversion cost to improve readability. Revisit a
specific ID only when mixing is demonstrated or a narrower seam emerges.

## Backend Sealed Outcome Policy

Use sealed outcomes for a genuinely closed set of expected states when callers branch and each state
owns different data. Keep ordinary return values and exceptions when no exhaustive branch exists;
wrapping every operation in success/failure records would make interfaces shallower, not safer.

Movie search reindex jobs are the first adopted state model. `Running` owns current progress,
`Completed` owns progress plus a finish time, and `Failed` owns progress, finish time, and an error
message. The public response remains unchanged, but its conversion uses an exhaustive pattern switch.
Adding a state now fails compilation until the response mapping handles it, and combinations such as
a running job with a finish time or a completed job with an error cannot be represented internally.

## Backend Property-Test Policy

Use jqwik for pure algorithms whose invariants span many combinations and whose failures benefit from
shrinking. Keep focused example tests for named edge cases and use integration tests for framework and
adapter behavior. Property tests must state a domain invariant rather than mirror the implementation.

The first properties cover movie-search rank fusion: a full result is the distinct union of both
rankings, and every page is a stable window over the full ranking. Together they check 600 generated
cases in about 0.36 seconds. A trial off-by-one mutation was detected and shrunk to an empty lexical
list plus the semantic list `[1]`; the mutation was reverted after proving the signal.

jqwik stores replay data under ignored `build/jqwik-database`, reports the reproduction seed on
failure, and retries the previous failing seed locally. Keep generated input sizes bounded so these
properties remain part of the fast backend lane.

## Backend Mutation-Testing Decision

Targeted mutation testing was trialed against `MovieSearchRankFusion`, including its example and
property tests. The trial used Gradle PIT plugin 1.19.0, PIT 1.25.5, and the stable JUnit Platform
adapter 1.2.3. PIT reached its coverage pre-scan on Java 25, but the adapter exposed both Jupiter and
jqwik engine descriptors as failing tests under the project's JUnit 6 runtime, even when only the
ordinary Jupiter example class was selected. The normal Gradle suite remained green.

The candidate is declined for now. Maintaining duplicate JUnit 4 tests or depending on an
unreleased adapter would reduce locality and add a second test model merely to operate the tool.
Revisit when a stable PIT adapter explicitly supports JUnit 6 and jqwik; retain the current property
tests meanwhile because their deliberate off-by-one mutation trial already proved useful fault
detection. Mutation testing should remain targeted and opt-in if it becomes compatible rather than
joining the fast `test` or `check` lanes.

## Backend Risk-Coverage Policy

JaCoCo branch verification protects compact, high-risk pure logic where every branch represents a
reviewable contract. The initial gate covers `RatingScore`, `MovieSearchRankFusion`, and
`ContentRecommendationRanker` at 100 percent branch coverage. The recommendation trial added tests
for diversity, missing search projections, sparse metadata on either side of a comparison, different
types and eras, and rating-confidence fallbacks; its branch coverage increased from 66 to 100
percent. A negative control that removed the null-score test reduced `RatingScore` to 7 of 8 covered
branches and made the verification task fail; the test was then restored.

`jacocoTestCoverageVerification` reads only the fast `test` execution data and is part of `check`.
The combined `jacocoTestReport` continues to include fast and integration execution data. Do not add
a repository-wide coverage percentage: generated mappers, framework adapters, and configuration
would turn it into a volume metric. Add a class only when its decisions are high risk, its tests can
exercise the branches through stable behavior, and complete coverage remains readable rather than
requiring implementation-shaped assertions.

## Backend Testcontainers Startup Decision

Parallel startup was trialed with Testcontainers `Startables.deepStart` for the independent
PostgreSQL, OpenSearch, and RustFS containers. With images cached, a targeted `DatabaseSchemaTest`
method completed in 35 seconds under the existing sequential lifecycle. The parallel version did not
complete after more than four minutes: container logs showed PostgreSQL ready and OpenSearch green,
but the combined startup future did not return and had to be interrupted.

The candidate is declined. A startup optimization that can hang the entire integration feedback loop
is worse than the small theoretical latency reduction, especially on a resource-constrained developer
machine starting OpenSearch beside two other services. Keep the shared singleton containers and
sequential startup. This decision does not prohibit parallel test execution research for tests that do
not share mutable infrastructure; it only rejects concurrent startup of this three-service fixture.

## Frontend Indexed Access Policy

TypeScript enables `noUncheckedIndexedAccess`, so array, tuple, regular-expression group, and other
indexed reads include `undefined` unless their presence is proven.

The adoption trial found eleven unsafe assumptions across production code, tests, and the frontend
architecture helper. Production fixes now:

- read the first uploaded file through the nullable `FileList.item()` interface and verify the
  `FileReader` result is a string;
- return `null` explicitly when random watchlist selection cannot produce an item;
- distinguish a missing daily featured movie from a movie whose generated transport ID is absent.

Tests and architecture helpers use optional access or explicit guards so a missing call, capture
group, or path segment still fails visibly. Avoid non-null assertions that merely silence this check;
narrow the value or define the appropriate absence behavior instead.

## Frontend Optional Property Policy

TypeScript enables `exactOptionalPropertyTypes`, so an optional property means the property may be
absent. It does not also mean that callers may explicitly assign `undefined` unless the declared
type says so.

The adoption trial exposed unsafe or misleading shapes in request payloads, optimistic cache data,
passkey options, telemetry events, and component boundaries. Prefer omitting unavailable values,
especially for transport objects. Include `| undefined` explicitly only at boundaries where passing
`undefined` is an intentional and supported operation, such as a presentation component that treats
an unavailable image token as its fallback state.

OpenAPI Generator 7.19.0 emits a `Configuration` constructor that assigns absent parameters as
`undefined`. The checked-in `frontend/openapi-templates/typescript-axios/configuration.mustache`
override preserves absence instead. `yarn run build:moviesGen` applies that template automatically;
generated output remains unedited and ignored.

## Frontend Type-Check Boundaries

`yarn typecheck` checks three environments independently:

- `tsconfig.json` checks browser application code and Vitest tests under `src`;
- `tsconfig.node.json` checks the Node-side Vite configuration;
- `tsconfig.e2e.json` checks Playwright configuration and end-to-end specs with Node and DOM types.

The production build runs all three checks before Vite builds. Playwright commands check the e2e
project before starting a browser because Playwright transpiles TypeScript but does not perform a
complete type check itself.

The adoption trial found three previously invisible issues: a Vite plugin invocation whose CommonJS
signature required an options object, a Playwright config that explicitly passed an absent worker
count, and an e2e helper whose inferred fixture type rejected the partial fixture it was meant to
exercise.

## Frontend Type-Aware Lint Policy

ESLint applies typescript-eslint's `recommendedTypeCheckedOnly` rules using the same three explicit
TypeScript projects as the compiler. It also enables exhaustive switch checking. This makes promise
lifecycle mistakes, unsafe `any` flow, invalid typed operations, and incomplete discriminated-union
switches fail during linting.

The adoption trial initially reported 56 findings. Low-signal rules for unnecessary assertions and
intentionally async functions without `await` are disabled, as are test-only checks that conflict
with mock method references and matcher values. The remaining 39 findings led to explicit
fire-and-forget navigation, awaited query invalidation, safer React event adapters, a typed slider
boundary, a corrected action-icon union, and profile-crop failure handling.

Use `void` only when deliberately discarding a promise whose rejection is already handled or whose
API owns error reporting. Await work that affects mutation lifecycle or cache consistency, and add an
explicit rejection path for operations that can fail independently.

## Frontend Runtime Validation Policy

Static transport types do not validate values received at runtime. Use Zod at high-impact external
boundaries where malformed data could corrupt security or application state, but do not duplicate
every generated OpenAPI model with a handwritten schema.

The first adopted boundary is authenticated session data. Password login, passkey login, and session
bootstrap all parse the same required session shape before it can populate auth state: a positive
integer account ID, non-empty username, valid email address, and string roles. Invalid bootstrap data
fails closed to an anonymous session; interactive login paths use their existing error feedback.
Unknown response fields are discarded so additive backend changes remain compatible.

Login navigation state is also parsed before constructing a post-authentication destination. Invalid
history state falls back to the home page. Current local-storage values already pass through explicit
allow-lists, so adding schemas there would not improve safety. Broad response validation, structured
error parsing, and environment schemas should be added only when a concrete failure mode warrants
their maintenance cost.

The production bundle trial moved about 17 KB gzip into the shared startup chunk while removing about
16 KB from the lazy identity chunk, increasing total compressed JavaScript by roughly 1.6 KB. This is
accepted for the authentication boundary and should be remeasured if runtime schemas expand.

## OpenAPI Response Contract Policy

Generated frontend types are only as precise as the backend's OpenAPI contract. Mark response fields
with `@Schema(requiredMode = REQUIRED)` when the backend guarantees that the field is present and
non-null. Use `nullable = true` in addition when a required field is always present but may contain
`null`. Leave fields optional when omission is part of the contract.

Adopt this incrementally while working in a domain rather than annotating every response type
mechanically. Required and nullable semantics need domain review, and annotation noise is not useful
when the guarantee is uncertain. A focused converter test should protect important schema contracts.

The first adopted contract is `AccountSessionResponse`. Its ID, username, email, and roles are now
required in the generated TypeScript interface, so consumers and coding agents get compile-time
feedback if they construct or assume an incomplete authenticated session. Runtime validation remains
valuable at this trust boundary because generated TypeScript types cannot validate server data.

## References

- [Gradle test execution](https://docs.gradle.org/current/userguide/java_testing.html)
- [Gradle JaCoCo plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [JUnit tags](https://docs.junit.org/current/user-guide/#running-tests-tags)
- [Testcontainers parallel startup](https://java.testcontainers.org/features/advanced_options/#parallel-container-startup)
- [Vitest parallelism](https://vitest.dev/guide/parallelism)
- [typescript-eslint typed linting](https://typescript-eslint.io/getting-started/typed-linting/)
- [TypeScript `noUncheckedIndexedAccess`](https://www.typescriptlang.org/tsconfig/noUncheckedIndexedAccess.html)
- [TypeScript `exactOptionalPropertyTypes`](https://www.typescriptlang.org/tsconfig/exactOptionalPropertyTypes.html)
- [Playwright TypeScript](https://playwright.dev/docs/test-typescript)
- [NullAway](https://github.com/uber/NullAway)
- [Error Prone](https://errorprone.info/)
