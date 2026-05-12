# API Contract Review

## Scope

Review backend REST controllers, DTOs, OpenAPI output, generated frontend client, and frontend API wrappers.

Primary files:

- `src/main/java/com/thecodinglab/imdbclone/controller`
- `src/main/java/com/thecodinglab/imdbclone/payload`
- `src/main/java/com/thecodinglab/imdbclone/exception`
- `frontend/src/client/imdb-clone-backend.yaml`
- `frontend/src/client/movies/MoviesApi.ts`
- `frontend/src/client/movies/generator-output`
- `frontend/src/features/**/api`
- `docs/frontend-data-contract.md`

## Checks

### Backend Contract

- controllers are thin and delegate business behavior
- DTOs are explicit API shapes, not accidental entity exposure
- request validation is applied where invalid input would reach services
- pagination shape is consistent
- auth requirements match route semantics and frontend expectations
- ProblemDetail/global exception style is consistent
- endpoint naming is stable and not leaking persistence names unnecessarily

### OpenAPI and Generated Client

- backend contract changes are reflected in `frontend/src/client/imdb-clone-backend.yaml`
- generated files under `frontend/src/client/movies/generator-output` are not manually edited
- `MoviesApi.ts` remains the frontend entrypoint over generated Axios code
- generated types are not copied into feature-local duplicates without reason

### Frontend Usage

- feature API wrappers call `MoviesApi.ts` rather than raw generated internals
- auth token handling is centralized
- frontend does not rely on backend implementation details not present in OpenAPI
- UI limits match backend constraints, especially page size and public/private route behavior
- image token semantics remain consistent with MinIO handling

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
