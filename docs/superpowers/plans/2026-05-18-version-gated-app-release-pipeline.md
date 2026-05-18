# Version-Gated App Release Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a GitHub Actions release pipeline that publishes backend/frontend images and updates k3s manifests only when the root app version changes.

**Architecture:** A root `VERSION` file is the single app release source of truth. The CD workflow detects whether `VERSION` changed on `master`; if it did, it builds `v<version>` backend/frontend images, resolves their digests, edits the home-cluster manifests, and commits those manifest updates back to `master` for Argo CD to deploy.

**Tech Stack:** GitHub Actions, Docker Buildx, Docker Hub, Bash, `sed`, `docker buildx imagetools`, kustomize manifests.

---

## File Structure

- Create `VERSION`: single shared backend/frontend app version.
- Modify `.github/workflows/continuous-deployment.yaml`: replace manual SHA/latest-only publishing with version-gated release deployment.
- Modify `infrastructure/clusters/home/apps/backend.yaml`: initially align the manifest image tag with `VERSION` if needed.
- Modify `infrastructure/clusters/home/apps/frontend.yaml`: initially align the manifest image tag with `VERSION` if needed.
- Modify `.github/workflows/README.md`: document the version-bump release workflow.

## Task 1: Add the Version Source

**Files:**
- Create: `VERSION`

- [ ] **Step 1: Add root version file**

Create `VERSION` with the currently deployed app version:

```text
0.2.1
```

- [ ] **Step 2: Verify version format**

Run:

```bash
test "$(cat VERSION)" = "0.2.1"
```

Expected: command exits `0`.

- [ ] **Step 3: Commit**

```bash
git add VERSION
git commit -m "chore: add app version source"
```

## Task 2: Replace CD Workflow with Version-Gated Release

**Files:**
- Modify: `.github/workflows/continuous-deployment.yaml`

- [ ] **Step 1: Replace workflow trigger and permissions**

Set the workflow header to:

```yaml
name: CD - Versioned app release

on:
  push:
    branches:
      - master
    paths:
      - VERSION
      - .github/workflows/continuous-deployment.yaml
  workflow_dispatch:

permissions:
  contents: write
```

- [ ] **Step 2: Add one release job**

Replace the two existing jobs with one `release-app` job. It must:

```yaml
jobs:
  release-app:
    runs-on: ubuntu-latest
    if: github.event_name == 'workflow_dispatch' || github.event.head_commit.message != 'chore(release): update app image digests [skip ci]'
```

- [ ] **Step 3: Checkout with full history**

Use:

```yaml
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          token: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 4: Read and validate `VERSION`**

Add:

```yaml
      - name: Read version
        id: version
        run: |
          version="$(tr -d '[:space:]' < VERSION)"
          if ! printf '%s' "$version" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
            echo "VERSION must be semver without leading v, got: $version" >&2
            exit 1
          fi
          echo "value=$version" >> "$GITHUB_OUTPUT"
          echo "tag=v$version" >> "$GITHUB_OUTPUT"
```

- [ ] **Step 5: Set up Docker Buildx and Docker Hub login**

Use the existing actions:

```yaml
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
```

- [ ] **Step 6: Build and push backend image**

Use:

```yaml
      - name: Build and push backend image
        run: |
          docker buildx build \
            --platform linux/amd64 \
            -t "${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-backend:${{ steps.version.outputs.tag }}" \
            -t "${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-backend:latest" \
            --push \
            .
```

- [ ] **Step 7: Build and push frontend image**

Use:

```yaml
      - name: Build and push frontend image
        working-directory: frontend
        run: |
          docker buildx build \
            --platform linux/amd64 \
            -t "${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-frontend:${{ steps.version.outputs.tag }}" \
            -t "${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-frontend:latest" \
            --push \
            .
```

- [ ] **Step 8: Resolve image digests**

Use:

```yaml
      - name: Resolve image digests
        id: digests
        run: |
          backend_image="${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-backend:${{ steps.version.outputs.tag }}"
          frontend_image="${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-frontend:${{ steps.version.outputs.tag }}"
          backend_digest="$(docker buildx imagetools inspect "$backend_image" --format '{{json .Manifest.Digest}}' | tr -d '"')"
          frontend_digest="$(docker buildx imagetools inspect "$frontend_image" --format '{{json .Manifest.Digest}}' | tr -d '"')"
          echo "backend=${backend_digest}" >> "$GITHUB_OUTPUT"
          echo "frontend=${frontend_digest}" >> "$GITHUB_OUTPUT"
```

- [ ] **Step 9: Update manifests**

Use:

```yaml
      - name: Update home-cluster app image manifests
        run: |
          backend_ref="${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-backend:${{ steps.version.outputs.tag }}@${{ steps.digests.outputs.backend }}"
          frontend_ref="${{ secrets.DOCKERHUB_USERNAME }}/imdb-clone-frontend:${{ steps.version.outputs.tag }}@${{ steps.digests.outputs.frontend }}"
          sed -i "s|image: .*imdb-clone-backend:.*|image: ${backend_ref}|" infrastructure/clusters/home/apps/backend.yaml
          sed -i "s|image: .*imdb-clone-frontend:.*|image: ${frontend_ref}|" infrastructure/clusters/home/apps/frontend.yaml
```

- [ ] **Step 10: Validate manifests**

Use:

```yaml
      - name: Validate kustomize render
        run: kubectl kustomize infrastructure/clusters/home/apps >/tmp/home-apps.yaml
```

- [ ] **Step 11: Commit manifest update if changed**

Use:

```yaml
      - name: Commit manifest update
        run: |
          if git diff --quiet infrastructure/clusters/home/apps/backend.yaml infrastructure/clusters/home/apps/frontend.yaml; then
            echo "Manifests already point at ${{ steps.version.outputs.tag }}"
            exit 0
          fi
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git add infrastructure/clusters/home/apps/backend.yaml infrastructure/clusters/home/apps/frontend.yaml
          git commit -m "chore(release): update app image digests [skip ci]"
          git push
```

- [ ] **Step 12: Validate workflow syntax locally**

Run:

```bash
git diff --check
```

Expected: no output.

- [ ] **Step 13: Commit**

```bash
git add .github/workflows/continuous-deployment.yaml
git commit -m "ci: release app images from version"
```

## Task 3: Document the Release Workflow

**Files:**
- Modify: `.github/workflows/README.md`

- [ ] **Step 1: Replace manual CD description**

Document:

```markdown
## Version-Gated App Releases

`VERSION` is the shared backend/frontend app version. Pushes to `master`
run CI, but app images are published only when `VERSION` changes or the CD
workflow is manually dispatched.

For version `0.2.2`, the workflow publishes:

- `niklastiede/imdb-clone-backend:v0.2.2`
- `niklastiede/imdb-clone-frontend:v0.2.2`

After publishing, the workflow resolves Docker digests and commits updates to
`infrastructure/clusters/home/apps/backend.yaml` and `frontend.yaml`. Argo CD
then deploys those manifest changes.
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/README.md
git commit -m "docs: document versioned app releases"
```

## Task 4: Final Verification

**Files:**
- Verify all changed files.

- [ ] **Step 1: Render kustomize**

Run:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-kustomize.yaml
```

Expected: command exits `0`.

- [ ] **Step 2: Check workflow YAML parses as YAML**

Run:

```bash
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/continuous-deployment.yaml")'
```

Expected: command exits `0`.

- [ ] **Step 3: Inspect git status**

Run:

```bash
git status --short
```

Expected: clean tree.
