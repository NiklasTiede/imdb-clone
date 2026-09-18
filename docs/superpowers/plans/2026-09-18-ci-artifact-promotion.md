# CI Artifact Promotion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and verify release images once in CI, then publish those exact images from CD without rebuilding or repeating the backend test suite.

**Architecture:** A `VERSION` push to `master` runs the normal CI gates and exports the three smoke-tested local images as short-lived GitHub Actions artifacts. A successful `workflow_run` starts CD at the exact upstream commit, downloads and loads those archives, retags them for Docker Hub, and updates the GitOps manifests with the published digests. Manual dispatch remains a complete build-and-test fallback.

**Tech Stack:** GitHub Actions, Docker image archives, Gradle, Vite, uv, Ruby workflow contract tests.

**Spec:** `infrastructure/clusters/home/tests/verify_release_workflows.rb`

## Global Constraints

- Only a successful CI `push` run for `master` may trigger automatic publication.
- CD must check out `workflow_run.head_sha` and download artifacts from `workflow_run.id`.
- Automatic CD must never rebuild an image or rerun application tests.
- Manual CD must retain the complete backend, frontend, and agent verification path.
- Release archives expire after one day and contain no source secrets.

---

### Task 1: Define the release artifact contract

**Files:**
- Modify: `infrastructure/clusters/home/tests/verify_release_workflows.rb`

**Interfaces:**
- Consumes: GitHub workflow YAML as plain text.
- Produces: A deterministic contract for the CI trigger, artifact names, upstream run identity, and manual fallback.

- [x] **Step 1: Replace the no-master-push assertion with a `VERSION`-scoped master push assertion.**
- [x] **Step 2: Require three SHA-scoped release artifacts with one-day retention.**
- [x] **Step 3: Require successful `workflow_run` gating, exact-SHA checkout, cross-run downloads, and image loading.**
- [x] **Step 4: Require test/build commands in CD to be manual-dispatch-only.**
- [x] **Step 5: Run `make verify-release-workflows` and confirm the new contract fails.**

### Task 2: Export verified CI images

**Files:**
- Modify: `.github/workflows/continuous-integration.yaml`
- Modify: `Makefile`

**Interfaces:**
- Consumes: `popcorn-society-{backend,frontend,agent}:local` after their smoke tests.
- Produces: `release-{service}-${GITHUB_SHA}` containing `{service}-image.tar.gz`.

- [x] **Step 1: Add the `VERSION`-scoped `master` push trigger.**
- [x] **Step 2: Give the agent image the repository `VERSION` through `AGENT_APP_VERSION`.**
- [x] **Step 3: Export each smoke-tested image with `docker save | gzip -1`.**
- [x] **Step 4: Upload each archive only for push runs using `actions/upload-artifact@v7`, zero additional compression, and one-day retention.**
- [x] **Step 5: Run `actionlint` and keep the contract test failing until CD is implemented.**

### Task 3: Promote exact images in CD

**Files:**
- Modify: `.github/workflows/continuous-deployment.yaml`

**Interfaces:**
- Consumes: the three SHA-scoped image artifacts from the successful upstream CI run.
- Produces: versioned and `latest` Docker Hub tags plus digest-pinned GitOps manifests.

- [x] **Step 1: Trigger from completed CI runs on `master` and retain manual dispatch.**
- [x] **Step 2: Gate automatic execution on successful push CI and check out its exact head SHA.**
- [x] **Step 3: Download the three artifacts with `actions: read`, `workflow_run.id`, and `GITHUB_TOKEN`; load them with Docker.**
- [x] **Step 4: Gate the existing tool setup, tests, and local image builds to manual dispatch.**
- [x] **Step 5: Retag and push the loaded local images, resolve digests, and retain the existing manifest validation and release PR flow.**
- [x] **Step 6: Run `make verify-release-workflows`, `actionlint`, and local archive round-trip smoke checks.**
