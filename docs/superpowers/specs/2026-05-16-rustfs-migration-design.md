# RustFS Migration Design

## Context

The project currently uses MinIO as local and deployed object storage. The MinIO
GitHub repository is archived as of April 25, 2026, so new runtime deployments
should move to an actively maintained S3-compatible object storage service.

RustFS is the target replacement. Its official documentation describes RustFS as
S3-compatible, provides a `rustfs/rustfs` Docker image, exposes the S3 API on
port `9000`, exposes the console on port `9001`, and documents use with the
MinIO Client (`mc`). This project can reseed object data, so the migration does
not need to preserve the current MinIO volume layout.

## Current Coupling

Runtime MinIO references exist in these areas:

- Root local compose file: `compose.yaml`
- Development and production deployment compose files:
  `infrastructure/deployment/development/docker-compose.yaml` and
  `infrastructure/deployment/production/docker-compose.stateful-apps.yaml`
- Production backend and frontend storage addresses:
  `infrastructure/deployment/production/docker-compose.stateless-apps.yaml`
- Backend storage code under `src/main/java/com/thecodinglab/imdbclone/media`
- Backend media tests and shared containers under `src/test/java`
- Frontend public image URL helpers under `frontend/src/shared/media`
- Seed scripts and docs under `infrastructure/minio`, `Makefile`, `README.md`,
  and deployment docs

The backend only uses simple S3 operations through `io.minio:minio`: create or
check bucket, set bucket policy, put object, remove object, stat object in tests,
and generate presigned GET URLs. That keeps the protocol compatibility risk
small. The long-term client target is AWS SDK for Java 2.x because it is the
maintained general Java S3 client; AWS SDK for Java 1.x is end-of-support and
must not be introduced.

## Decision

Migrate runtime object storage from MinIO to RustFS in two phases:

1. Replace the MinIO runtime with RustFS and keep the existing MinIO Java SDK as
   the first-step S3 client.
2. Replace the MinIO Java SDK with AWS SDK for Java 2.x after RustFS runtime
   compatibility is proven.

Rename project-facing concepts from "MinIO" to "object storage" or "RustFS"
where the name describes runtime infrastructure.

This gives the project an actively maintained storage runtime with the smallest
possible first-step application behavior change, then removes the remaining
MinIO-branded Java dependency in a separate, testable phase.

## Architecture

RustFS becomes the stateful object storage service in local, development, and
production compose files:

- Service port: `9000`
- Console port: `9001`
- Image: `rustfs/rustfs`
- Data mount: `/data`
- Access key env var: `RUSTFS_ACCESS_KEY`
- Secret key env var: `RUSTFS_SECRET_KEY`
- Console enablement: `RUSTFS_CONSOLE_ENABLE=true`

Backend application properties keep the stable prefix
`imdb-clone.media.storage.*`. These properties describe the application contract,
not the product implementation. Metadata and comments should describe the values
as S3-compatible object storage instead of MinIO-specific storage.

The final backend client implementation should use AWS SDK for Java 2.x:

- `software.amazon.awssdk:s3` as the Gradle dependency
- `S3Client` for bucket, policy, put, delete, and stat operations
- `S3Presigner` for temporary GET URLs
- `endpointOverride(URI.create(properties.uri()))` for RustFS
- `forcePathStyle(true)` for local and S3-compatible endpoint compatibility
- a fixed signing region such as `us-east-1`, because AWS SDK v2 requires a
  region even when using a custom endpoint

The frontend should move from `VITE_IMDB_CLONE_MINIO_ADDRESS` to
`VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS`. During migration, the helper should
accept the new variable first and fall back to the old variable, then to
`http://localhost:9000`. This keeps local setups and Playwright config from
breaking while the deployment variables are updated.

Seed jobs continue to use `mc` because RustFS documents compatibility with it.
The alias name should be renamed from `minio` to `rustfs`, but commands still
create the `imdb-clone` bucket and copy the movie images into the same object key
layout.

## Data Flow

The backend starts with `imdb-clone.media.storage.uri` pointing to RustFS. On
media infrastructure setup, it checks for the `imdb-clone` bucket, creates it if
missing, and applies the existing public read policy for:

- `arn:aws:s3:::imdb-clone/movies/*`
- `arn:aws:s3:::imdb-clone/profile-photos/*`

Movie and profile image uploads continue to create resized image objects under
the same key layout. The frontend continues to construct direct public object
URLs under `/imdb-clone/movies/...` and `/imdb-clone/profile-photos/...`.

Because reseeding is acceptable, local and deployed RustFS volumes can be new
volumes. The migration does not attempt to mount or convert the old
`imdb-clone-minio-data` volume.

## Error Handling

Existing storage failures currently surface through `MinioOperationException`.
The migration should introduce a vendor-neutral exception name, such as
`ObjectStorageOperationException`, and update global error logging to say
"object storage" instead of "MinIO".

The first migration should preserve the current HTTP behavior: storage operation
failures return `500 Internal Server Error` through the existing ProblemDetail
style. Any retry, fallback, or background repair behavior is out of scope.

## Testing Strategy

Backend media integration tests should run against RustFS, not MinIO. Replace
the Testcontainers `MinIOContainer` with a generic RustFS container that exposes
port `9000`, sets RustFS credentials, and starts with `/data`.

Keep existing integration assertions for:

- Store movie image writes both expected objects
- Replacing a movie image deletes old objects
- Deleting movie image clears token and removes objects
- Store profile photo writes both expected objects
- Replacing profile photo deletes old objects
- Deleting profile photo clears token and removes objects
- Account deletion and movie deletion clean up corresponding media objects

Add or update frontend tests so image URL helpers prefer
`VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS`, still support the old MinIO env var as
a temporary fallback, and fall back to `http://localhost:9000`.

After the AWS SDK v2 client migration, media integration tests should assert
object existence through `S3Client.headObject`. Missing-object assertions should
expect an AWS SDK `S3Exception` with an S3 error code equivalent to `NoSuchKey`
or a `404` status code, depending on what RustFS returns through AWS SDK v2.

Manual verification should start the compose stack, upload or seed objects,
confirm the backend can apply the bucket policy, and confirm a browser can load
a direct public object URL.

## Scope

In scope:

- Runtime replacement from MinIO to RustFS in local and deployment compose files
- Testcontainers replacement for media integration tests
- Java storage client replacement from MinIO Java SDK to AWS SDK for Java 2.x
- Seed command updates while keeping the existing object key layout
- Vendor-neutral naming in app-facing config comments, metadata, frontend env
  variables, exceptions, and docs
- Reseeding object data into a new RustFS volume

Out of scope:

- In-place conversion of the existing MinIO volume
- Changing bucket names or object key layout
- Changing image processing behavior
- Introducing RustFS distributed or multi-node deployment topology
- Adding new backup or observability infrastructure

## Rollout

1. Update local compose and media integration tests to RustFS.
2. Verify backend media integration behavior against RustFS.
3. Rename Java error handling and project-facing storage names to object storage.
4. Replace MinIO Java SDK usage with AWS SDK for Java 2.x.
5. Verify media integration behavior still passes against RustFS through AWS SDK
   v2.
6. Rename frontend media URL helpers while keeping backward compatible frontend
   env fallback.
7. Update development and production deployment compose files and seed jobs.
8. Update documentation and generated credential names.
9. Recreate or remove old MinIO volumes and reseed RustFS.

## Open Risks

- RustFS must accept the current S3 bucket policy format used by
  `config/minio-policy.json`.
- Direct public browser URLs must work after bucket policy application.
- `mc` seed commands must work with the RustFS container before the backend
  starts depending on seeded objects.
- Testcontainers startup may need a wait strategy based on RustFS logs or HTTP
  readiness instead of the MinIO-specific container helper.
- AWS SDK v2 may expose RustFS error responses differently than the MinIO Java
  SDK, so tests should assert stable S3 semantics instead of exact SDK exception
  classes wherever possible.
