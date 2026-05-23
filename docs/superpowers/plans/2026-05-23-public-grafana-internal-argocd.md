# Public Grafana Internal Argo CD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose Grafana publicly for demo viewers and expose Argo CD permanently for LAN-only operator access.

**Architecture:** Grafana remains part of the existing `observability` Helm application, gains a public Traefik/cert-manager ingress, and gets a SOPS-backed viewer account created by an idempotent Kubernetes Job. Argo CD gets a separate GitOps-managed ingress in the `argocd` namespace, protected by a Traefik LAN IP allowlist and backed by HTTPS to the existing `argocd-server` service.

**Tech Stack:** Argo CD, Traefik CRDs, cert-manager, SOPS, kube-prometheus-stack Grafana chart, Grafana HTTP Admin API.

---

### Task 1: Public Grafana Viewer Access

**Files:**
- Create: `infrastructure/clusters/home/apps/observability-grafana-viewer.sops.yaml`
- Modify: `infrastructure/clusters/home/apps/kustomization.yaml`
- Modify: `infrastructure/clusters/home/apps/observability.yaml`

- [ ] **Step 1: Add a SOPS-encrypted viewer secret**

Create `observability-grafana-viewer.sops.yaml` containing a Kubernetes Secret named `observability-grafana-viewer` in namespace `observability` with encrypted `stringData` keys:

```yaml
viewer-login: viewer
viewer-email: grafana-viewer@imdb-clone.local
viewer-password: random 32-byte base64 value generated with openssl before SOPS encryption
```

- [ ] **Step 2: Enable Grafana public ingress**

In `observability.yaml`, set Grafana ingress to host `grafana.imdb-clone.the-coding-lab.com`, Traefik ingress class, cert-manager `letsencrypt-prod`, and TLS secret `imdb-clone-grafana-tls`.

- [ ] **Step 3: Add viewer bootstrap job**

In `observability.yaml` `extraManifests`, add an idempotent PostSync Job named `observability-grafana-viewer-user` that:

- waits for `http://observability-grafana.observability.svc.cluster.local/api/health`
- authenticates with the SOPS-backed admin secret
- looks up `viewer`
- creates the user if absent
- resets the password to the SOPS value
- enforces org role `Viewer`

### Task 2: LAN-Only Argo CD Access

**Files:**
- Create: `infrastructure/clusters/home/apps/argocd-ingress.yaml`
- Modify: `infrastructure/clusters/home/apps/kustomization.yaml`

- [ ] **Step 1: Add Traefik middleware and transport**

Create a Traefik `Middleware` named `argocd-lan-only` in namespace `argocd` with `ipAllowList.sourceRange` for `192.168.178.0/24` and loopback ranges.

Create a Traefik `ServersTransport` named `argocd-server` in namespace `argocd` with `insecureSkipVerify: true`, because Traefik talks HTTPS to Argo CD's internal self-signed server certificate.

- [ ] **Step 2: Add Argo CD ingress**

Create a standard Kubernetes `Ingress` named `argocd-internal` in namespace `argocd` for `argocd.imdb-clone.the-coding-lab.com`, using:

- `ingressClassName: traefik`
- cert-manager issuer `letsencrypt-prod`
- Traefik middleware annotation `argocd-argocd-lan-only@kubernetescrd`
- Traefik backend scheme `https`
- Traefik servers transport `argocd-argocd-server@kubernetescrd`
- backend service `argocd-server`, port `https`

### Task 3: Documentation And Verification

**Files:**
- Modify: `infrastructure/kubernetes/README.md`

- [ ] **Step 1: Document public and restricted hosts**

Add `grafana.imdb-clone.the-coding-lab.com` as a public demo host and `argocd.imdb-clone.the-coding-lab.com` as a LAN-restricted operator host.

- [ ] **Step 2: Verify Kustomize and Helm rendering**

Run:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
helm template observability kube-prometheus-stack \
  --repo https://prometheus-community.github.io/helm-charts \
  --version 85.2.2 \
  --namespace observability \
  --values /tmp/imdb-clone-observability-values.yaml \
  >/tmp/imdb-clone-observability-rendered.yaml
```

Expected: both commands exit `0`, rendered manifests contain the Grafana ingress, viewer bootstrap job, Argo CD ingress, LAN middleware, and backend ServiceMonitor.
