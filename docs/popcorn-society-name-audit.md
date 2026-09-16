# Remaining project-name audit — 2026-09-16

The product and source project are **Popcorn Society**. This audit covers tracked source,
configuration, CI/CD, Compose, Kubernetes/Helm/Argo resources, operational scripts, dashboards,
current documentation, filenames, compatibility code and historical artifacts. Private settings
and decrypted secrets are not part of a mechanical rename.

Reproduce the inventory with `make audit-project-names`. It prints only file/line locations and
matched project identifiers. Its categories are location-based triage, not an assertion that an
arbitrary new occurrence is safe. Review new matches using the retention reasons below.
The desired result is **no accidental old branding**, not zero occurrences of the letters IMDb.

## Implemented in this pass

| Area | Result / activation |
| --- | --- |
| Java metrics and trace attributes | `popcorn_society.frontend.*`, `.assistant.*`, `.search.*`, `.rate_limit.*`; becomes active with new backend image |
| Python metrics | `popcorn_society_agent_*`; becomes active with new agent image |
| Agent service identity | `popcorn-society-agent`, including health/build info |
| Frontend telemetry identity | `popcorn-society-frontend` |
| Backend service identity | Default `popcorn-society-backend`; release updater changes Spring and Pyroscope together |
| Dashboard/alert queries | New metric names with old-name fallback inside rate/increase, before aggregation; old, new and mixed rollouts remain visible |
| Grafana folders/viewer | `Popcorn Society`, `Popcorn Society Data`, `Popcorn Society Viewer` for newly created viewer accounts |
| Browser preferences/IDs | New `popcorn-society` keys; valid legacy values copied on first read, new values win, old tabs can still read legacy values |
| Future release images | CD builds/pushes `niklastiede/popcorn-society-{backend,frontend,agent}`; tested updater pins the published digests in the deployment PR |
| Runtime environment names | Release updater changes `IMDB_CLONE_*`/`IMDB_AGENT_*` when replacing old images; inactive domain component already uses new names |
| Local Compose | Project, services, containers, network and logical volume keys now `popcorn-society-*`; physical volumes still point at existing data |
| Local tools | Make commands, media-upload script, seed User-Agent headers, temporary output paths and E2E container names updated |
| Operator tunnels | New state directory for fresh sessions; existing legacy PID files remain discoverable until tunnels are stopped |
| Ansible | Own variable/checksum annotation names updated; real host/repository addresses retained |
| Media migration seam | Frontend bucket is configurable through `VITE_POPCORN_SOCIETY_OBJECT_STORAGE_BUCKET`; existing bucket remains default |

No version bump, deployment, DNS change, GitHub rename, Docker Hub publication, data move or
secret replacement happened. Changes in **active** dashboard/alert manifests can be reconciled by
Argo CD as soon as merged; they intentionally support the old production images too.

## Remaining names: retain until the corresponding object is migrated

| Remaining references | Why they are still required | Removal / migration gate |
| --- | --- | --- |
| `imdb-clone` Kubernetes namespace; workload/service/SA/NetworkPolicy names and selectors | Existing deployment identities; renaming creates different objects, and namespace migration affects secrets and quota storage | Separate application namespace migration with validated secrets, PVC data and routing; see below |
| Database Helm release names, service names, PVCs, secret references | Existing PostgreSQL/OpenSearch/RustFS data; a release rename can allocate empty PVCs | Keep stateful installation initially; use new Service aliases or a separately verified data restore |
| Current `imdb-clone-*` app image refs | The immutable 1.6.1 images exist under those repositories | Next new VERSION/CD run publishes the new repositories and the deployment PR replaces all three refs |
| `niklastiede/imdb-clone-seed` | Independently versioned, published catalog/media seed artifacts | Publish and verify the corresponding light/full tags under a new repo before changing consumers |
| Physical local Docker volume names | They hold existing developer data | Optional explicit offline copy to new volumes; logical names are already clean, so this is not required for development |
| Storage bucket `imdb-clone` and public object URLs | Existing media lives at those addresses | Copy objects, retain policy/content metadata, compare contents; switch all readers/writers together, retain old bucket for cached URLs |
| Old app/media/backend/Grafana/Argo hostnames | Actual DNS, TLS, OAuth, WebAuthn and external URLs | Public domain procedure in the migration runbook; separate media/operator hostname changes |
| GitHub/Codecov/badge/Argo source URLs | Actual repository is still `NiklasTiede/imdb-clone` | Rename repository on GitHub, then update remote, Argo/Ansible source URL, badges, Codecov and external links |
| Dashboard UIDs and navigation paths | Existing bookmarks/links and provisioned dashboards | Stable opaque IDs can remain indefinitely; a rename creates new dashboard identities and needs coordinated link/bookmark updates |
| Old metrics in new dashboard queries | Historical data and currently running old images | Remove fallbacks only after rollout and desired historical retention window; queries prefer new series for identical labels |
| Environment aliases, old frontend address-default keys | Existing private environment files and old images | Migrate private overrides first; changing checked-in Vite defaults to new names prematurely masks old private overrides |
| Legacy Java serialized class names and binary fixture | Persisted scheduled tasks must still deserialize | Keep reader compatibility until old payloads cannot exist, including restored backups |
| Old browser storage key literals | Required to copy existing IDs/preferences | Retain for the compatibility period; storage cannot transfer automatically across different domains |
| Historic plans/reviews/Compose/Swarm configs and diagram | Describe the actual earlier system | Keep as historical evidence; do not run archived deployment configurations against current production |

Genuine `imdb_id`, `imdb_rating`, dataset import columns, external IMDb links and source attribution
are intentionally outside this rename. Existing Flyway migrations and production data are unchanged.

## Local Compose transition

Because the Compose project and container names changed, stop the old project before starting
the new one. Otherwise the old services still occupy ports 5432/9000/9200/8082. Do not run both
projects against the same writable volumes.

Use the pre-change Compose file to stop its project (without deleting volumes), then run
`make docker-compose-dev-up` with the new file. For this branch's pre-change checkpoint:

```bash
git show c50a41ba:compose.yaml > /tmp/popcorn-previous-compose.yaml
docker compose -p imdb-clone -f /tmp/popcorn-previous-compose.yaml down
make docker-compose-dev-up
```

Confirm the old project name with `docker compose ls` first if it was overridden locally.
Never add `--volumes` to that shutdown. These commands are instructions, not actions performed
by the rebranding change. Existing explicit physical volume names are preserved byte-for-byte.

## Remaining production migration order

1. **Application release:** create/authorize the three public Docker Hub repositories if the token
   cannot create repositories; choose a new unused VERSION, run CD and merge its deployment PR.
   The updater validates every input/source manifest before writing, supports both old/new refs,
   and leaves namespace, services, PVCs, backend addresses and secrets untouched.
2. **Public domain:** follow `popcorn-society-migration.md` for DNS, OAuth, TLS, passkey re-enrollment,
   preview, release-time canonical URL and redirects. The domain component requires compatible
   new images. Keep the old domain redirect and certificate operational.
3. **GitHub repository:** rename only after the release is stable; then update local remotes,
   Argo root/seed repo URLs, Ansible, README badges and Codecov integration. Verify Argo fetches
   the new URL before relying on it. Do not assume a local directory rename renames GitHub.
4. **Media:** create a new bucket with the same public prefixes, copy existing objects preserving
   metadata, and verify counts/checksums and representative URLs. Switch backend bucket setting,
   frontend build-time bucket setting, seed runtime, bucket-init policy and media import tooling
   together. Include profile photos, not just seeded movie posters. Keep the old bucket available
   for cached URLs until verified unused. A bucket rename is not an S3 metadata rename.
5. **Kubernetes application namespace:** create `popcorn-society` alongside the old namespace;
   re-encrypt SOPS secrets for their new metadata using SOPS (never edit namespace around encrypted
   values/MAC). Create renamed Services, service accounts, workloads, RBAC, monitor selectors and
   NetworkPolicies as a coordinated set. Retain access to the existing databases namespace through
   explicit service addresses. Stop the old agent before copying the voice quota SQLite database
   and associated state into the new namespace's PVC; do not run two independent quota stores.
   Keep the scheduler/backend cutover controlled. Verify health, MCP, voice and network isolation,
   then switch ingress backends/middleware namespace references and monitoring selectors. Retain
   the old installation for a compatible rollback; remove it only after explicit review.
6. **Stateful services (optional):** prefer neutral/new Service aliases while keeping existing
   Helm/PVC identities. If physical names must change, use verified backup/restore or an offline
   volume copy and an explicit maintenance window. Never apply a global replace to active Helm
   release names, PVCs or SOPS resources.
7. **Compatibility cleanup:** after the rollout/retention gates above, remove aliases and fallback
   queries individually and rerun this inventory. The repository's local folder can then be renamed
   outside the active editor/processes; update IDE paths and saved project configuration.

The large number of remaining grep hits is chiefly repeated references to a small set of actual
runtime identities, historical records and compatibility reads. None can be safely removed by
pretending the corresponding external resource has already changed its name.

## Verification of this pass

- `./gradlew spotlessApply test architectureTest` and `./gradlew build jacocoTestReport`: passed;
  245 unit, 27 architecture and 234 integration tests executed, two optional tests skipped.
  The first complete build exposed stale Spotless incremental output ("0 lint errors");
  `./gradlew spotlessJava --rerun-tasks spotlessJavaCheck` passed and the normal full build then
  passed without disabling checks or changing the formatter configuration.
- `make verify-agent`: passed, including Ruff, strict typing, architecture, 426 deterministic tests
  and evals. `make docker-build-agent && make container-smoke-agent`: passed.
- Frontend `yarn lint`, `yarn test --maxWorkers=2`, `yarn build`: passed, 447 tests.
- `make verify-kubernetes-schema verify-popcorn-migration`: passed; 43 schema-valid resources,
  24 custom schemas skipped, zero invalid/errors. Existing semantic CRD checks passed.
- `make verify-metric-branding`: promtool validates 54 actual dashboard queries plus agent rules;
  old-only, new-only, mixed-pod and overlapping-name scenarios passed. Included in the schema gate.
- `make verify-observability-charts`: chart renders/schema checks and Alloy validation passed.
- `make verify-release-workflows`: workflow contracts and release updater tests passed. A temporary
  copy with new image refs/env names also passed Movie Concierge, runtime-hardening and observability
  contracts under strict release verification. Preview-domain production contracts passed too.
- `docker compose config --quiet`: passed; resolved old/new Compose configurations retain the same
  four physical volume names. Bash syntax checks for operator/media-upload scripts passed.
- Movie-seed unit tests: 26 tests, three environment-dependent skips; dev-media generator: three
  passed. Existing Flyway migrations, encrypted SOPS files and VERSION have no diff.
- `git diff --check HEAD`: passed. The audit command was rerun over the final working tree.

Not run: live deployment, remote registry publication, OAuth/voice provider calls or domain checks.
Browser E2E and backend/frontend container rebuilds were not repeated: this pass changes neither
UI interactions nor those runtime images' packaging; unit/integration/build gates cover the edits.
Ansible was inspected but not applied to the host. Runtime resource and DNS transitions remain
separate operations described above. Changes are prepared locally, not committed or pushed.
