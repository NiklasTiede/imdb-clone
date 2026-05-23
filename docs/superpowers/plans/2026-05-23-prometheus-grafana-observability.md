# Prometheus Grafana Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first observability slice for the home k3s cluster: Prometheus, Grafana, Kubernetes/node metrics, and Spring Boot backend metrics.

**Architecture:** Deploy `kube-prometheus-stack` as an Argo CD Helm application in the existing `observability` namespace. Keep Grafana, Prometheus, and Alertmanager internal-only, use local-path persistence for Prometheus and Grafana, and define the backend `ServiceMonitor` as a Helm extra manifest so it is applied after the monitoring CRDs exist.

**Tech Stack:** Argo CD, Helm, prometheus-community/kube-prometheus-stack 85.2.2, Prometheus Operator, Grafana, kube-state-metrics, node-exporter, Spring Boot Actuator/Micrometer.

---

### Task 1: Add kube-prometheus-stack

**Files:**
- Create: `infrastructure/clusters/home/apps/observability.yaml`
- Create: `infrastructure/clusters/home/apps/observability-grafana-admin.sops.yaml`
- Modify: `infrastructure/clusters/home/apps/kustomization.yaml`

- [ ] **Step 1: Create the Argo CD application**

Add `infrastructure/clusters/home/apps/observability.yaml` with an Argo CD `Application` named `observability`. Configure:

- Helm repo: `https://prometheus-community.github.io/helm-charts`
- Chart: `kube-prometheus-stack`
- Version: `85.2.2`
- Destination namespace: `observability`
- Grafana ingress disabled
- Prometheus ingress disabled
- Alertmanager ingress disabled
- Prometheus retention `7d`, retention size `6GiB`, local-path PVC `10Gi`
- Grafana local-path PVC `2Gi`
- Resource requests and limits for Grafana, Prometheus, Alertmanager, Prometheus Operator, kube-state-metrics, and node-exporter
- k3s-incompatible control-plane component scrapes disabled: etcd, controller-manager, scheduler, kube-proxy
- Grafana admin credentials from existing SOPS Secret `observability-grafana-admin`

- [ ] **Step 2: Add backend metrics ServiceMonitor**

In the same `observability.yaml`, use `extraManifests` to create a `monitoring.coreos.com/v1` `ServiceMonitor` in namespace `imdb-clone`.

It must select:

```yaml
selector:
  matchLabels:
    app.kubernetes.io/name: imdb-clone-backend
endpoints:
  - port: management
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

- [ ] **Step 3: Wire the app into Kustomize**

Add `observability.yaml` to `infrastructure/clusters/home/apps/kustomization.yaml`.
Add `observability-grafana-admin.sops.yaml` to `infrastructure/clusters/home/apps/kustomization.yaml` before `observability.yaml` so the secret exists before the Helm chart uses it.

### Task 2: Document Access

**Files:**
- Modify: `infrastructure/kubernetes/README.md`

- [ ] **Step 1: Add internal access commands**

Document SSH port-forward access for Grafana:

```bash
ssh -L 3001:localhost:3001 robotnik@um560 \
  "sudo kubectl -n observability port-forward svc/observability-grafana 3001:80"
```

Document how to retrieve the generated Grafana admin password without printing any secret value in the docs:

```bash
ssh robotnik@um560 \
  "sudo kubectl -n observability get secret observability-grafana-admin \
    -o jsonpath='{.data.admin-password}' | base64 -d"
```

### Task 3: Verify

**Files:**
- Read-only validation only

- [ ] **Step 1: Render the GitOps tree**

Run:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
```

Expected: command exits `0`.

- [ ] **Step 2: Confirm the observability app is present**

Run:

```bash
rg -n "name: observability|chart: kube-prometheus-stack|targetRevision: 85.2.2|kind: ServiceMonitor|/actuator/prometheus" /tmp/imdb-clone-home-apps.yaml
```

Expected: matching lines for the Argo CD application and backend ServiceMonitor.

- [ ] **Step 3: Template the Helm chart values**

Extract the `valuesObject` from the Argo CD application into a temporary values file, then run:

```bash
helm template observability kube-prometheus-stack \
  --repo https://prometheus-community.github.io/helm-charts \
  --version 85.2.2 \
  --namespace observability \
  --values /tmp/imdb-clone-observability-values.yaml \
  >/tmp/imdb-clone-observability-rendered.yaml
```

Expected: command exits `0` and rendered output contains `Prometheus`, `Grafana`, and `ServiceMonitor` resources.
