# GitOps And k3s Review

## Scope

Review the home-cluster GitOps tree, Argo CD application model, SOPS secret flow,
ingress/cert-manager resources, image/version deployment path, and Kubernetes
manifest verification.

Primary files:

- `infrastructure/clusters/home/apps`
- `infrastructure/clusters/home/root-app.yaml`
- `infrastructure/ansible`
- `.github/workflows/continuous-deployment.yaml`
- `Makefile`
- `VERSION`
- `.sops.yaml`
- `infrastructure/kubernetes/README.md`
- `docs/development.md`
- `docs/agents/verification.md`

## Checks

### GitOps Ownership

- the root Argo CD app points at the expected repo revision and app path
- app resources under `infrastructure/clusters/home/apps` are included in `kustomization.yaml`
- each app has a clear namespace owner and does not rely on manual post-apply changes
- generated or runtime-only resources are not committed unless intentionally GitOps-owned
- local Compose configuration and k3s manifests do not drift on ports, service names, bucket names, or index names

### Secrets And Config

- Kubernetes secrets remain SOPS-encrypted as `*.sops.yaml`
- plaintext secret values are not committed in manifests, docs, scripts, dashboards, or examples
- SOPS decryption path is documented for Argo CD and does not require application source changes
- ConfigMaps hold non-secret runtime config only
- frontend public config is separated from backend secrets

### Images And Releases

- image tags or digests are updated by the intended release path
- manifest image references are pinned enough for repeatable deploys
- `VERSION` and CD workflow behavior match the documented release model
- app manifests do not point to stale local-only images

### Ingress, Certificates, And Exposure

- public hosts are intentional and documented
- cert-manager issuers, TLS secrets, ingress class, and Traefik middleware names are consistent
- internal-only services are not exposed through public ingress by accident
- LAN-only routes such as Argo CD keep their access controls

### Verification

- `kubectl kustomize infrastructure/clusters/home/apps` renders without applying
- `make verify-kubernetes-schema` or an equivalent kubeconform check validates rendered manifests
- manifest contract checks assert required resources by `(apiVersion, kind, namespace, name)`
- live-cluster checks, when requested, are kept separate from normal CI

## Suggested Contract Tests

- required namespaces exist in rendered output
- required Argo CD `Application` resources exist for frontend, backend, data services, and observability
- each `*.sops.yaml` resource listed in the app tree remains encrypted
- backend and frontend ingress hosts match documented public URLs
- observability resources are included in kustomization before dependent apps use them
- backend service exposes the expected app and management ports

## Report Guidance

Prefer GitOps failure scenarios:

- "Argo CD cannot deploy this resource because it is not included in the app kustomization."
- "This secret would be plaintext in Git."
- "The rendered manifest points at an image tag that the release workflow no longer updates."
- "The public ingress exposes an operator-only service."

Separate static manifest concerns from live-cluster operational status. Do not require
live cluster access unless the user explicitly asks for smoke checks.
