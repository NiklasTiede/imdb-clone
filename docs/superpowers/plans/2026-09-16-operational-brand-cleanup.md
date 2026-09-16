# Popcorn Society operational naming cleanup

> Implementation plan. Execute inline in the existing `feature/popcorn-society-rebrand` branch.
> Design/requirements: `docs/popcorn-society-migration.md`; owner requested an exhaustive
> audit and implementation, preserving actual IMDb data and existing persistent data.

## Design

Use `popcorn-society` for project/service identifiers, `popcorn_society` for metrics,
`POPCORN_SOCIETY` for environment variables, and Popcorn Society for display names.
Source changes and operational cutover have different activation boundaries. Do not rename a
reference to an externally existing object without also migrating that object. Do not rewrite
historical records, genuine IMDb catalog fields, old serialized class descriptors or encrypted
SOPS files. No live deployment, destructive migration, external rename, or version bump here.

## Tasks

- [x] Audit all tracked text, filenames, resource identities, external addresses and persisted keys;
  record actionable groups and explicit retained references in `docs/popcorn-society-name-audit.md`.
- [x] Rename Java/Python metrics and frontend/agent telemetry display identities. Make active
  dashboard/alert metric selectors cover both generations through the release. Keep Kubernetes
  label selectors and dashboard UIDs stable for now. Rename Grafana folder/viewer display names.
- [x] Move three browser storage keys with tested migration-on-read, retaining voice browser IDs.
  Preserve old/new environment compatibility. Parameterize the frontend bucket path.
- [x] Rename local Compose service/container/network/logical-volume identifiers and matching Make,
  tooling and docs references. Pin existing physical volume names; document stop-before-recreate.
  Rename scratch outputs and independent CI container names. Retain published seed references.
- [x] Publish future application images as `popcorn-society-*`. Add a tested release-manifest updater
  that accepts old/new source image references, pins published digests, and changes runtime env
  keys/telemetry only together with compatible new images. Keep current pinned images untouched.
- [x] Document exact remaining production resource/bucket/GitHub/registry migration order and
  compatibility removal gates. Add a reproducible audit that distinguishes retained references
  from accidental newly introduced project names.
- [x] Verify backend, agent, frontend, release updater, Compose, Kubernetes, staged domain migration
  and monitoring queries. Review final diff for persisted data and deployment activation hazards.

## Verification

Run targeted browser storage tests, release-updater fixtures (old and new repositories, failure
atomicity), `./gradlew spotlessApply build jacocoTestReport`, `make verify-agent`, frontend lint,
tests with two workers and build, `make verify-kubernetes-schema verify-popcorn-migration`,
`make verify-observability-charts` and Compose config validation. Use promtool for changed rules
and dual-name metric query behavior. Do not call production endpoints or mutate live resources.

## Audit decisions

- Active K8s namespace, workload/service names, Helm release names, PVCs and SOPS metadata remain
  deployment identities until an explicit migration. Replacing them in the active GitOps tree
  would create a second installation, not rename the existing one.
- Current DNS, WebAuthn RP ID, media bucket and published immutable images remain actual addresses.
- Archived docs/Compose/Swarm artifacts describe prior installations and are not active branding.
- Legacy environment aliases and projection deserialization names are intentional compatibility.

## Completion evidence

See `docs/popcorn-society-name-audit.md` for the delivered changes, actual checks and retained
production migration gates. No remote resources were mutated.
