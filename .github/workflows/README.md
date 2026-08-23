
# Current Git Workflow

For the beginning I will just work directly on the
master-branch. CI will be triggered with every
push.

CI verifies the Java backend, React frontend, and Python Movie Concierge independently. The Agent
job installs the locked Python 3.14 environment, runs the strict verification gate, builds the
non-root image, and executes its container smoke test without provider credentials.

## Version-Gated App Releases

`VERSION` is the shared backend/frontend/Movie Concierge app version. Pushes to `master`
run CI, but app images are published only when `VERSION` changes or the CD
workflow is manually dispatched.

For version `0.2.2`, the workflow publishes:

- `niklastiede/imdb-clone-backend:v0.2.2`
- `niklastiede/imdb-clone-frontend:v0.2.2`
- `niklastiede/imdb-clone-agent:v0.2.2`

The release job runs the deterministic Python gate before publishing. It builds the agent with the
shared release version embedded in `imdb_agent_build_info`; provider and MCP secrets are never
available to GitHub Actions and are not needed for build or verification.

After publishing, the workflow resolves Docker digests and commits updates to
`infrastructure/clusters/home/apps/backend.yaml`, `frontend.yaml`, and `agent.yaml`. Argo CD
then deploys those manifest changes.

The first concierge pilot branch includes the next `VERSION` and an image tag matching it. Merging
that branch is therefore the explicit release action; merely pushing the feature branch does not
publish an image or mutate the cluster.

The Playwright e2e workflow is also manual-only. It starts
PostgreSQL, OpenSearch, Object Storage, the Spring Boot backend, and the
Vite frontend before running the browser tests. This keeps the
regular push CI fast while still making full-stack browser checks
available before larger merges or releases.

There's only one kubernetes 'production' namespace 
on Kubernetes.

# Planned Feature-Branch Git Workflow

In the future, trunk-based development should be introduced.
Feature branches which are connected to issues will be created 
and merged with pull requests after a review process. These 
short-lived branches will enable merging small changes
continuously and keeping master branch as source of truth.

Furthermore, two Kubernetes namespaces (production and 
integration) will be created with a master- and a develop-branch 
so that changes can be thoroughly tested on integration 
environment before it can be released on production.
