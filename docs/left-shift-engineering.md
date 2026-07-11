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
| Candidate | Add Error Prone | Catch semantic Java mistakes during compilation. Verify Java 25 compatibility first. |
| Candidate | Add JSpecify and NullAway incrementally | Make nullability part of each checked module interface. Start with pure packages. |
| Candidate | Introduce high-value domain types | Prevent mixing IDs and constructing invalid ratings, years, or tokens. |
| Candidate | Model expected outcomes with sealed types where callers branch | Make new expected states require exhaustive handling. |
| Candidate | Add property-based tests for pure invariants | Exercise ranking, pagination, conversion, hashing, and aggregate behavior over many inputs. |
| Candidate | Add targeted mutation testing | Prove important tests fail when implementation behavior is changed incorrectly. |
| Candidate | Add risk-based JaCoCo branch thresholds | Turn coverage regressions in important modules into build failures. |
| Candidate | Start independent Testcontainers concurrently | Reduce some integration startup latency without parallel database mutation. |
| Candidate | Consolidate avoidable Spring test contexts | Reduce repeated application-context and connection-pool startup. |
| Candidate | Parallelize only the fast backend lane | Use measured Gradle forks without duplicating containers or Spring context caches. |
| Candidate | Enable dependency locking and verification | Make transitive dependency resolution reproducible and checksum-verified. |
| Candidate | Replace source-text architecture checks where practical | Verify module dependencies and seams semantically rather than through formatting-sensitive scans. |

## Frontend Opportunities

| Status | Experiment | Expected early feedback |
| --- | --- | --- |
| Adopted | Enable `noUncheckedIndexedAccess` | Detect possibly missing array and indexed values; the adoption trial found eleven issues. |
| Adopted | Enable `exactOptionalPropertyTypes` | Distinguish omitted properties from explicit `undefined`; a local generator template keeps generated configuration compatible. |
| Adopted | Type-check Vite, Playwright, and end-to-end files | Bring TypeScript outside `src` into the normal build gate; the adoption trial found three issues. |
| Adopted | Enable type-aware typescript-eslint rules | Catch floating promises, unsafe values, misused promises, and non-exhaustive handling; the curated adoption trial found 39 actionable issues. |
| Candidate | Parse trust seams with runtime schemas | Validate HTTP responses, errors, router state, storage, and environment values before use. |
| Candidate | Strengthen OpenAPI required and nullable contracts | Generate useful transport types instead of making most response fields optional. |
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
