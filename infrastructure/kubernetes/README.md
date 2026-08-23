This folder tracks the Kubernetes migration for the home-server deployment.

Target first milestone:

- Ansible bootstraps Ubuntu 24 LTS on `robotnik@um560`.
- k3s runs as a single-node Kubernetes cluster.
- k3s bundled Traefik and local-path storage stay enabled initially.
- Argo CD is installed but not exposed publicly.
- Argo CD watches `infrastructure/clusters/home/apps`.

Bootstrap from the repository root:

```bash
cd infrastructure/ansible
ansible-playbook site.yml
```

Verify the cluster over SSH:

```bash
ssh robotnik@um560
sudo kubectl get nodes
sudo kubectl get pods -A
sudo kubectl get applications -n argocd
```

Open Argo CD privately through SSH port-forwarding:

```bash
ssh -L 8080:localhost:8080 robotnik@um560 \
  "sudo kubectl -n argocd port-forward svc/argocd-server 8080:443"
```

Then open `https://localhost:8080` locally. The initial admin password can be read on the server:

```bash
sudo kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d
```

## GitOps Secrets

SOPS with age is used for encrypted Kubernetes secrets committed to Git.

- The age private key lives locally at `.secrets/sops/age/keys.txt`.
- `.secrets/` is ignored by Git and must be backed up in a private password/secret manager.
- Ansible uploads that key into the `argocd` namespace as the `sops-age-key` Secret.
- Argo CD repo-server uses the `sops-kustomize` config management plugin to decrypt
  `*.sops.yaml` files during sync.

Generate a local age key if it does not exist:

```bash
mkdir -p .secrets/sops/age
docker run --rm -v "$PWD/.secrets/sops/age:/keys" alpine:3.20 \
  sh -c "apk add --no-cache age >/dev/null && age-keygen -o /keys/keys.txt"
```

After changing the age key or plugin config, rerun:

```bash
cd infrastructure/ansible
ansible-playbook site.yml
```

## Movie Seed Job

`seed-job.example.yaml` documents the Kubernetes Job shape for the versioned full
seed image. Copy it into the GitOps app tree once PostgreSQL and RustFS services
exist in k3s, replace the image tag and secret names, then let Argo CD apply it.

The seed job is idempotent. Rerunning it upserts movie rows and uploads media
objects without deleting existing catalog data.

After the seed job completes, rebuild the OpenSearch movie index explicitly
through the backend admin endpoint. The backend does not reindex movies as an
application startup side effect.

The home-cluster GitOps tree also contains `rustfs-bucket-job.yaml`, an
idempotent Argo CD hook that creates the `imdb-clone` bucket and makes
`imdb-clone/movies/*` publicly readable before seeded media is served.

## Public Hosts

The home-cluster ingress exposes these public hostnames:

- `imdb-clone.the-coding-lab.com` for the frontend
- `backend.imdb-clone.the-coding-lab.com` for the backend API
- `object-storage.imdb-clone.the-coding-lab.com` for public movie media
- `grafana.imdb-clone.the-coding-lab.com` for public read-only observability dashboards

These DNS records should point to the current public home IP. The router
must forward TCP ports `80` and `443` to the k3s node at `192.168.178.44`.

The `argocd.imdb-clone.the-coding-lab.com` hostname is also routed through
Traefik, but it is protected by a Traefik IP allowlist for the home LAN
`192.168.178.0/24` and the current public home IP. The public IP entry allows
home Wi-Fi access through router NAT loopback, where Traefik may see the
router's public address instead of the laptop's LAN address. The k3s Traefik
service uses `externalTrafficPolicy: Local` so Traefik can evaluate the client
source IP instead of the k3s service proxy IP.

## HTTPS Certificates

The home cluster uses cert-manager with a Let's Encrypt `ClusterIssuer`.
Certificates are requested from the public ingresses through HTTP-01 challenges
handled by Traefik. Keep both TCP `80` and `443` forwarded from the router to
`192.168.178.44`; port `80` is required for initial issuance and renewal.

## Observability

Grafana and Prometheus run inside the `observability` namespace through the
`observability` Argo CD application. Grafana is exposed publicly at
`https://grafana.imdb-clone.the-coding-lab.com` with a read-only `viewer`
account. Retrieve the generated viewer password from the cluster:

```bash
ssh robotnik@um560 \
  "sudo kubectl -n observability get secret observability-grafana-viewer \
    -o jsonpath='{.data.viewer-password}' | base64 -d"
```

Grafana can still be opened privately through an SSH tunnel:

```bash
ssh -L 3001:localhost:3001 robotnik@um560 \
  "sudo kubectl -n observability port-forward svc/observability-grafana 3001:80"
```

Then open `http://localhost:3001`. The Grafana admin user is `admin`. Retrieve
the generated admin password from the cluster:

```bash
ssh robotnik@um560 \
  "sudo kubectl -n observability get secret observability-grafana-admin \
    -o jsonpath='{.data.admin-password}' | base64 -d"
```

The initial Prometheus setup collects Kubernetes node and workload metrics,
kubelet/cAdvisor metrics, kube-state-metrics, and the backend Spring Boot
Actuator endpoint at `/actuator/prometheus`.

## Movie Concierge production pilot

The Movie Concierge runs as one hardened Python pod in the existing `imdb-clone` namespace. It has
no database, PVC, second namespace, or user-specific credentials. Browser traffic stays same-origin:
Traefik routes `/concierge-api/v1` to the agent and strips `/concierge-api`; health and Prometheus
endpoints are cluster-internal only. Its `Recreate` update strategy prevents two independent
in-memory session and budget ledgers from overlapping, at the cost of a short rollout interruption.

`movie-concierge-runtime.sops.yaml` owns exactly two encrypted values:

- the OpenAI project key, projected only into the Python pod;
- one random MCP bearer token, projected into both Python and Java using different filenames.

Edit or rotate this file only through SOPS with the ignored age key:

```bash
SOPS_AGE_KEY_FILE=.secrets/sops/age/keys.txt \
  sops infrastructure/clusters/home/apps/movie-concierge-runtime.sops.yaml
```

Never decrypt it into the repository, shell history, logs, screenshots, or documentation. Set the
dedicated OpenAI project to a hard $20 pilot budget before release; the in-memory agent ledger is a
defense-in-depth limit and resets with the pod.

The Agent NetworkPolicy allows ingress only from Traefik and Prometheus. Egress is limited to
cluster DNS, Java MCP on port 8080, and public non-private IPv4 destinations on HTTPS port 443. The
pod runs as UID/GID 10001 with no service-account token, no privilege escalation or Linux
capabilities, RuntimeDefault seccomp, a read-only root filesystem, and a 16 MiB memory-backed `/tmp`.

Prometheus scrapes `/metrics` through an internal ServiceMonitor. The Grafana sidecar loads the
`IMDb Clone / Movie Concierge` dashboard. Prometheus alert rules are installed, but Alertmanager is
intentionally disabled, so there is no notification delivery yet.

Validate locally without touching the cluster:

```bash
make verify-movie-concierge-production
make verify-kubernetes-schema
```

Do not use `kubectl apply` for a release. This pilot branch carries the next shared `VERSION`; merge
it only as an intentional production release after review. That master push starts the version-gated
workflow, which tests and publishes backend, frontend, and agent images, resolves immutable digests,
and commits the three GitOps manifest updates for Argo CD. Before that merge, no workflow or cluster
deployment is triggered.

Argo CD is exposed for home LAN access at
`https://argocd.imdb-clone.the-coding-lab.com`. The route is intended for
operator use only and is restricted by Traefik middleware.
