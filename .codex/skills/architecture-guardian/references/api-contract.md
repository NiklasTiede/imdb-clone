# API Contract Review

## Scope

Review backend REST controllers, DTOs, OpenAPI output, generated frontend client, and frontend API wrappers.

Primary files:

- `src/main/java/com/thecodinglab/imdbclone/*/web`
- `src/main/java/com/thecodinglab/imdbclone/*/api`
- `src/main/java/com/thecodinglab/imdbclone/shared/api`
- `src/main/java/com/thecodinglab/imdbclone/shared/error`
- `frontend/src/client/imdb-clone-backend.yaml`
- `frontend/src/client/movies/MoviesApi.ts`
- `frontend/src/shared/api/moviesApi.ts`
- `frontend/src/client/movies/generator-output`
- `frontend/src/features/**/api`
- `docs/frontend-data-contract.md`

## Checks

### Backend Contract

- controllers are thin and delegate business behavior
- request/response records in module `api` packages are explicit API shapes, not accidental entity exposure
- request validation is applied where invalid input would reach services
- pagination shape is consistent
- auth requirements match route semantics and frontend expectations
- ProblemDetail/global exception style is consistent
- endpoint naming is stable and not leaking persistence names unnecessarily
- controllers live in module `web` packages, not a global technical-layer controller package

### OpenAPI and Generated Client

- backend contract changes are reflected in `frontend/src/client/imdb-clone-backend.yaml`
- generated files under `frontend/src/client/movies/generator-output` are not manually edited
- `shared/api/moviesApi.ts` owns generated Axios API class construction and HTTP client wiring
- `MoviesApi.ts` remains a compatibility entrypoint over the shared API wrappers
- generated DTO and enum types are not copied into feature-local duplicates without reason

### Frontend Usage

- feature API wrappers call the shared API wrapper exports rather than constructing raw generated API clients
- auth token handling is centralized
- frontend does not rely on backend implementation details not present in OpenAPI
- UI limits match backend constraints, especially page size and public/private route behavior
- image token semantics remain consistent with RustFS handling

### Drift Sources

Flag these as contract drift:

- DTO field exists in frontend docs but not generated OpenAPI
- frontend expects embedded movie data where backend only returns IDs
- backend changes enum values without regenerating the client
- docs or `AGENTS.md` list dependency versions that disagree with build metadata

## Verification Recommendations

- start backend and regenerate OpenAPI only during implementation, not in guardian mode
- compare generated OpenAPI to committed spec in a follow-up implementation task
- run `cd frontend && yarn build`
- add focused frontend API wrapper tests when a contract issue is found
