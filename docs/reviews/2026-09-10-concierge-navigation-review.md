# Concierge navigation pre-commit review

Reviewed all staged and unstaged changes against HEAD on `feature/agent-roadmap`, including the
Python text/voice adapters, application action contracts, frontend navigation/search, tests and docs.
No commit or deployment was performed. This review preserves ADRs 0001 and 0004.

## Findings corrected

1. **P2 — Intermediate searches could choose the wrong page.** Text and voice emitted the first
   successful search immediately. A title lookup followed by similar-movie recommendations opened
   the seed title's search; a refined second search or failed refinement left the earlier query
   selected. `concierge.navigation.SearchNavigation` now owns the decision behind one Interface
   shared by both Adapters. It tracks the latest tool call, rejects older results, invalidates an
   earlier candidate when another call starts, and emits only a successful final discovery search
   after the tool loop completes. Scripted text and real SDK voice-loop tests cover refinement,
   downstream recommendations, failure and late transcripts. A core test covers out-of-order results.

2. **P2 — The search bar could overwrite an agent request.** Reproduced in Chromium: submit an
   unchanged `Arrival` search, ask for `Forrest Gump`, then ask for `Arrival` again. The old pending
   query Set prevented input synchronization; the debounce restored `Forrest Gump` after 300 ms.
   AppBar now tracks one pending exact URL and does not register unchanged navigation. Regression
   coverage includes real route changes, text-input state and mobile/desktop voice E2E.

3. **P2 — Intent filtering rejected legitimate discovery requests.** `Never Let Me Go`, `Open Water`
   and “dramas without violence” were blocked by global word exclusions. The policy now distinguishes
   compound action clauses and negated commands from words inside a title or search description.
   These three cases failed before correction and now pass; existing mutation, conditional,
   negated-command and direct-movie-open cases remain covered. This is still a bounded English
   command grammar, not unrestricted natural-language understanding.

4. **P3 — Title-type filters were absent from the active filter count.** A type-only search had an
   active criterion but no desktop Clear all and zero mobile filters. The filter Module now owns
   its chip, deletion, count and clear behavior. MovieSearchPage only coordinates URL/query state.

5. **P3 — Text page/search navigation was counted as movie opening.** The browser telemetry contract
   currently supports only `OPEN_MOVIE`; the chat hook reported every action under that name.
   Reporting now emits this metric only for actual movie-open actions. SSE tests prove page/search
   routing still works without false movie metrics and genuine movie opens remain recorded.

## Architecture and DDD assessment

- Java remains the owner of Movie, Watchlist, Rating and Account behavior and authorization. No new
  database access, ranking engine, aggregate, persistence model or account authority was added to
  Python. Navigation is application orchestration; adding DDD repositories or aggregates here would
  not clarify a domain invariant.
- The Concierge Module owns command intent and navigation policy. Framework event adaptation and
  tool-result validation remain in the text/voice Adapters. `SearchNavigation` improves Locality:
  sequencing policy is tested through the same Interface used by both Adapters. Removing it would
  duplicate that policy at two real Seams.
- React owns route construction and validation. The browser receives typed semantic actions, never
  model-generated destinations. Pure page navigation stays separate from committed mutation receipts
  and Undo. Existing identity-scoped session disposal remains in effect.
- Search uses the existing URL and TanStack Query state instead of maintaining a second result store.
  Cross-feature imports use public Interfaces; shared route construction does not eagerly import the
  search feature. The full frontend suite includes feature-import and route-architecture checks.
- Python import contracts and architecture tests enforce the product/Adapter dependency direction.
  No architecture rewrite or additional framework is warranted for this change.

## Verification

Executed from the repository root unless a command changes directory:

| Check | Result |
| --- | --- |
| `make verify-agent` | Passed: format, lint, strict types, import contracts, 3 architecture tests, 258 tests, 27 deterministic eval cases |
| `cd agent && uv run pytest tests/concierge/test_navigation.py --cov=imdb_agent.concierge.navigation --cov-branch --cov-report=term-missing` | 34 passed; navigation Module: 53/53 statements and 12/12 branches covered |
| `cd frontend && yarn run lint` | Passed |
| `cd frontend && yarn test --maxWorkers=2` | 361 passed across 107 files, including architecture tests |
| `cd frontend && yarn build` | Passed, including application/tooling/E2E typechecks; existing bundle/lazy-import warnings remain |
| `cd frontend && yarn e2e e2e/concierge.spec.ts e2e/concierge-voice.spec.ts e2e/concierge-navigation.spec.ts e2e/movie-search.spec.ts --workers=2 --reporter=list` | 33 passed; 1 intentional skip for a mobile-only test in the desktop project |
| `git diff --check` | Passed |

Coverage percentages apply only to the named navigation Module and exercised paths. They do not
prove arbitrary language interpretation, model reliability or full application coverage. An attempted
whole-agent instrumented run failed during Pydantic RootModel import/collection with
`ValueError: tuple.index(x): x not in tuple`; the ordinary full test gate and isolated navigation
coverage run pass. A global Python coverage figure is therefore unavailable from this review.

Backend integration/container checks were not repeated: no Java contract, database, dependency,
container, startup configuration or packaging changed. Browser/provider replay evidence is recorded
separately below; deterministic tests remain independent of credentials and running infrastructure.

## Live replay evidence

A bounded Chromium session with generated English audio, real Grok, the Python service and the
running Java catalog passed all three commands on 2026-09-10, using one continuous WebSocket:

| Synthetic command | Observed action |
| --- | --- |
| “Search for Forrest Gump” | `show_search_results`, query visible in the normal search page |
| “Find movies similar to Forrest Gump” | Recommendation response without an additional navigation |
| “Open Forrest Gump” | `open_movie` for the actual catalog record, ID 6 |

In this single replay, action arrival was 8.487 s for search and 3.840 s for direct movie opening,
measured from the **start of input audio**. These include speaking time and are not end-of-speech
latencies or performance guarantees. Search navigation waits for generation/tool-loop completion;
it does not wait for the browser to finish playing the generated audio. No voice errors occurred.
An initial replay produced no ready event before its harness deadline during concurrent checks;
retrying after local service readiness passed. The harness and synthetic WAVs stayed outside Git.

## Follow-ups and practical limits

- Search page navigation now waits for the final tool-loop result. This can add generation time;
  streaming speech and the existing early path for an explicit movie-open request are unchanged.
- New action types have Python text emitted-action metrics, but comprehensive browser execution
  telemetry needs an explicit Java/browser vocabulary extension. Voice action success telemetry is
  also a separate follow-up; the current fix avoids reporting false movie opens.
- Python and TypeScript validate the action contract independently. Shared versioned fixtures or
  schema compatibility checks would improve drift detection as the vocabulary expands; current
  adapter, schema, SSE and browser tests cover the delivered cases.
- The broader contextual capability registry, trailer playback and unrestricted language support
  remain separate work. There are no unresolved blocking findings in the reviewed changes.


## Follow-up: ratings-list wording

The user subsequently reported “please open my ratings list”: the model acknowledged the request,
but the fixed page matcher accepted only “ratings” and “ratings page”. The earlier review had not
changed that matcher and missed these wording variants. Added explicit aliases for “ratings list”,
“ratingslist”, “rating list”, “rated movies” and “movie ratings” (including its page form).
Negated commands and combined deletion requests remain rejected. Text tests cover authenticated
navigation and anonymous login redirection; real SDK voice-loop tests cover tool-free page commands
with both timely and late final transcripts. `make verify-agent` passes with 271 tests, architecture
checks and all 27 evals. The local agent was restarted to load the fix. No frontend or Java changes
were needed for this correction.

## Follow-up: natural commands and conversational context

The fixed page matcher was also an execution bottleneck for paraphrases. Added session-local
`navigate_app`, `open_movie_page` and `show_movie_search` tools, shared by the text and realtime
adapters. The model interprets navigation intent; the tools accept only fixed destinations,
catalog-grounded movie IDs, or the latest successful validated search parameters. Existing direct
command fast paths remain. Local tools are kept out of the Java MCP tool/telemetry vocabulary.
Current final transcription, turn epoch, cancellation, evidence-before-action and one-action guards
still apply. Personal write receipts take precedence over navigation suggestions.

Personal action parsing now accepts conversational prefixes, “this one” / “that one”, natural
watchlist requests and rating assignments such as “I'd give it an eight”. It still binds the
operation, target and user-specified score independently of the model. After an incomplete rating
request for a grounded movie, the next user turn can supply just the score. This pending intent
expires on cancellation or an intervening turn. An opened movie becomes the following turn's
context, without discarding the current turn's full catalog candidates. Clarification messages ask
for the missing detail instead of prescribing a sentence template. ADR 0004 records this refinement.

`make verify-agent` passes: **316 tests**, three architecture tests, two import boundary contracts,
strict Pyright, Ruff formatting/lint and all 27 deterministic evals. `git diff --check` passes.
Deterministic SDK tests cover semantic page/movie actions in text and voice, including late final
transcripts. Additional tests cover ungrounded IDs, interrupted navigation, failed search refinements,
score-only clarification, stale context and receipts for conversational personal commands.
No frontend, Java, container or deployment changes were required for this follow-up; their suites
were not rerun. Personal mutations were tested deterministically, without changing a live account.

Live synthetic speech reached the actual home route for “I would like to get back to the main
screen”, opened the catalog's Forrest Gump record for “Let us have a look at that one”, and showed
COMEDY / runtime-under-90 search results for “I would like to see some comedy movies under ninety
minutes”. One initial title recording was transcribed as “forest gone”; a clearer recording with
the release year resolved Forrest Gump. One replay started before voice readiness and was retried.
The live test also exposed an anonymous policy that only asked for sign-in without invoking
navigation; that prompt now explicitly calls `navigate_app`, whose destination becomes login.
The final replay passed both the conversational comedy search and “I would like to see the movies
I have rated” -> `open_login` -> `/login`, in one continuous voice WebSocket without voice errors.

Navigation meaning and ambiguity resolution remain model-dependent; the deterministic checks
verify execution constraints, not universal language understanding. Personal writes remain bounded
by the supported affirmative grammar, with more natural wording and immediate score clarification.

## Commit preparation verification

Before committing the complete change set, reran `make verify-agent` (316 tests, architecture,
types, lint and 27 evals), `yarn lint`, `yarn test` (361 tests) and `yarn build`: all passed.
The build retains existing chunk-size and ineffective dynamic-import warnings.

`yarn e2e concierge-navigation.spec.ts --reporter=line` initially passed three of four cases;
the mobile guest case exceeded the five-second search-input assertion deadline while the other
checks ran in parallel. Its subsequent failure snapshot already showed the expected value and
results. After those checks completed, reran the full navigation spec with `--workers=1`:
all four guest/authenticated desktop/mobile cases passed. This records the timing sensitivity
rather than hiding the first result. Java and container checks were not rerun because this change
set does not modify Java code, dependencies or container definitions.
