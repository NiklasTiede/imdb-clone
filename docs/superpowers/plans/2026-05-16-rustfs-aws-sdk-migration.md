# RustFS and AWS SDK Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace MinIO runtime storage with RustFS and then replace the MinIO Java SDK with AWS SDK for Java 2.x.

**Architecture:** The first checkpoint swaps local/test object storage to RustFS while keeping the current MinIO Java client. The second checkpoint replaces the Java client with AWS SDK v2 `S3Client` and `S3Presigner`, preserving the existing `imdb-clone.media.storage.*` application properties and object key layout.

**Tech Stack:** Java 25, Spring Boot 4, Gradle, Testcontainers, RustFS, AWS SDK for Java 2.x, React 19, Vite, Vitest, Docker Compose, MinIO Client (`mc`) for seeding.

---

## File Structure

- Modify `compose.yaml`: replace the root local MinIO service with RustFS, keeping S3 on host port `9000`.
- Modify `src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java`: use a generic RustFS Testcontainer instead of `MinIOContainer`.
- Modify `build.gradle`: remove MinIO Java/Testcontainers MinIO dependencies and add AWS SDK v2 dependencies after the RustFS checkpoint.
- Modify `gradle.properties`: add `awsSdkVersion=2.44.4`.
- Rename `src/main/java/com/thecodinglab/imdbclone/media/internal/MinioClientConfig.java` to `ObjectStorageClientConfig.java`: provide `S3Client` and `S3Presigner`.
- Modify `src/main/java/com/thecodinglab/imdbclone/media/internal/MediaFiles.java`: replace `io.minio` calls with AWS SDK v2 calls.
- Rename `src/main/java/com/thecodinglab/imdbclone/shared/error/MinioOperationException.java` to `ObjectStorageOperationException.java`.
- Modify `src/main/java/com/thecodinglab/imdbclone/shared/error/GlobalExceptionHandler.java`: handle the vendor-neutral exception and log object storage failures.
- Modify `src/test/java/com/thecodinglab/imdbclone/media/MediaServiceIntegrationTest.java`: assert object state through `S3Client`.
- Modify `src/main/resources/config/minio-policy.json`: rename to `object-storage-public-read-policy.json`.
- Modify `src/main/resources/META-INF/additional-spring-configuration-metadata.json`: remove MinIO-specific descriptions.
- Modify `src/main/resources/config/application-dev.properties` and `src/main/resources/config/application-prod.properties`: rename comments from MinIO to object storage.
- Modify `frontend/src/shared/media/imageUrls.ts`: add object-storage names and keep temporary MinIO aliases.
- Modify `frontend/src/shared/media/imageUrls.test.ts`: test new env var, legacy fallback, and localhost fallback.
- Modify `frontend/src/shared/media/index.ts` and `frontend/src/vite-env.d.ts`: export and type object-storage names.
- Modify `frontend/.env.production`: use the Vite object-storage env variable for production builds.
- Modify `frontend/playwright.config.ts`: use the new Vite env var.
- Modify `infrastructure/deployment/development/docker-compose.yaml`: replace MinIO runtime with RustFS and update seed alias.
- Modify `infrastructure/deployment/production/docker-compose.stateful-apps.yaml`: replace MinIO runtime with RustFS and update seed alias.
- Modify `infrastructure/deployment/production/docker-compose.stateless-apps.yaml`: point backend at `imdb-clone-rustfs` and expose new frontend env var name.
- Modify `infrastructure/deployment/production/generate_credentials.sh`: rename generated storage env variables from MinIO to RustFS/object storage.
- Modify `infrastructure/minio/dev-seed/upload_to_minio.sh`: rename env variables and aliases while keeping `mc`.
- Modify docs: `README.md`, `AGENTS.md`, `infrastructure/README.md`, `infrastructure/deployment/README.md`, and targeted `infrastructure/minio` docs.

### Task 1: RustFS Local Runtime Checkpoint

**Files:**
- Modify: `compose.yaml`
- Modify: `src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java`
- Test: `src/test/java/com/thecodinglab/imdbclone/media/MediaServiceIntegrationTest.java`

- [ ] **Step 1: Replace root compose MinIO service with RustFS**

In `compose.yaml`, replace the MinIO block with:

```yaml
  ### ----------------------- RustFS ----------------------- ###
  imdb-clone-rustfs:
    container_name: imdb-clone-rustfs
    image: rustfs/rustfs:latest
    restart: unless-stopped
    user: root
    environment:
      - RUSTFS_ACCESS_KEY=ROOTNAME
      - RUSTFS_SECRET_KEY=CHANGEME123
      - RUSTFS_CONSOLE_ENABLE=true
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - imdb-clone-rustfs-data:/data
    networks:
      - imdb-clone-network
    command: /data
```

In the `volumes:` block, replace:

```yaml
  imdb-clone-minio-data:
    name: imdb-clone-minio-data
```

with:

```yaml
  imdb-clone-rustfs-data:
    name: imdb-clone-rustfs-data
```

- [ ] **Step 2: Replace MinIO Testcontainer with RustFS**

In `src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java`, replace the MinIO imports with:

```java
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
```

Replace the image and container fields:

```java
  private static final DockerImageName rustfsImage = DockerImageName.parse("rustfs/rustfs:latest");

  public static GenericContainer<?> rustfsContainer =
      new GenericContainer<>(rustfsImage)
          .withExposedPorts(9000)
          .withEnv("RUSTFS_ACCESS_KEY", "minioadmin")
          .withEnv("RUSTFS_SECRET_KEY", "minioadmin")
          .withEnv("RUSTFS_CONSOLE_ENABLE", "false")
          .withCommand("/data")
          .waitingFor(
              Wait.forHttp("/").forPort(9000).forStatusCodeMatching(code -> code < 500))
          .withStartupTimeout(Duration.ofMinutes(2));
```

Replace `minioProperties` with:

```java
  @DynamicPropertySource
  static void objectStorageProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "imdb-clone.media.storage.uri",
        () ->
            String.format(
                "http://%s:%d", rustfsContainer.getHost(), rustfsContainer.getMappedPort(9000)));
    registry.add("imdb-clone.media.storage.access-key", () -> "minioadmin");
    registry.add("imdb-clone.media.storage.secret-key", () -> "minioadmin");
    registry.add("imdb-clone.media.storage.bucket-name", () -> "imdb-clone");
  }
```

Replace `minioContainer.start();` with:

```java
    rustfsContainer.start();
```

- [ ] **Step 3: Run the existing media integration test against RustFS**

Run:

```bash
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
```

Expected: the test passes, proving RustFS works with the current MinIO Java SDK for the current media behavior.

- [ ] **Step 4: Commit the RustFS runtime checkpoint**

```bash
git add compose.yaml src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java
git commit -m "test(storage): run media tests on RustFS"
```

### Task 2: Vendor-Neutral Backend Names

**Files:**
- Rename: `src/main/java/com/thecodinglab/imdbclone/shared/error/MinioOperationException.java` to `src/main/java/com/thecodinglab/imdbclone/shared/error/ObjectStorageOperationException.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/media/internal/MediaFiles.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/shared/error/GlobalExceptionHandler.java`
- Rename: `src/main/resources/config/minio-policy.json` to `src/main/resources/config/object-storage-public-read-policy.json`
- Modify: `src/main/resources/META-INF/additional-spring-configuration-metadata.json`
- Modify: `src/main/resources/config/application-dev.properties`
- Modify: `src/main/resources/config/application-prod.properties`

- [ ] **Step 1: Rename the storage exception**

Move the file and replace its contents with:

```java
package com.thecodinglab.imdbclone.shared.error;

public class ObjectStorageOperationException extends RuntimeException {

  private final String message;
  private final Exception exception;

  public ObjectStorageOperationException(String message, Exception exception) {
    this.message = message;
    this.exception = exception;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public Exception getException() {
    return exception;
  }
}
```

- [ ] **Step 2: Update `MediaFiles` exception names and messages**

Replace the import:

```java
import com.thecodinglab.imdbclone.shared.error.MinioOperationException;
```

with:

```java
import com.thecodinglab.imdbclone.shared.error.ObjectStorageOperationException;
```

Replace every `MinioOperationException` reference with `ObjectStorageOperationException`.

Replace message suffixes such as:

```java
"Error while storing file in MinIO"
```

with:

```java
"Error while storing file in object storage"
```

Replace the bucket policy resource name:

```java
      String bucketPolicy = "config/minio-policy.json";
```

with:

```java
      String bucketPolicy = "config/object-storage-public-read-policy.json";
```

- [ ] **Step 3: Update global exception handling**

In `GlobalExceptionHandler`, replace the exception handler with:

```java
  @ExceptionHandler(ObjectStorageOperationException.class)
  protected final ProblemDetail resolveObjectStorageOperationException(
      ObjectStorageOperationException ex, WebRequest request) {
    logger.warn(
        "While interacting with object storage an error occurred with message: '{}' and '{}' on resource '{}' ",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(EXCEPTION_MESSAGE, ex.getException().getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }
```

- [ ] **Step 4: Rename the policy file**

Rename `src/main/resources/config/minio-policy.json` to:

```text
src/main/resources/config/object-storage-public-read-policy.json
```

Keep the JSON contents unchanged.

- [ ] **Step 5: Update storage metadata descriptions**

In `additional-spring-configuration-metadata.json`, replace the four media storage descriptions with:

```json
"description": "S3-compatible object storage endpoint URI used by the media module."
```

```json
"description": "S3-compatible object storage access key used by the media module."
```

```json
"description": "S3-compatible object storage secret key used by the media module."
```

```json
"description": "S3-compatible object storage bucket containing movie images and profile photos."
```

- [ ] **Step 6: Update backend property comments**

In `application-dev.properties` and `application-prod.properties`, replace:

```properties
### ------------ Minio Config -------------------------
```

with:

```properties
### ------------ Object Storage Config ----------------
```

- [ ] **Step 7: Format and run the narrow backend test**

Run:

```bash
./gradlew spotlessApply
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
```

Expected: formatting completes and the media integration test passes.

- [ ] **Step 8: Commit vendor-neutral backend names**

```bash
git add src/main/java/com/thecodinglab/imdbclone/media/internal/MediaFiles.java src/main/java/com/thecodinglab/imdbclone/shared/error src/main/resources/config src/main/resources/META-INF/additional-spring-configuration-metadata.json
git commit -m "refactor(storage): remove MinIO error naming"
```

### Task 3: AWS SDK v2 Client Migration

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Rename: `src/main/java/com/thecodinglab/imdbclone/media/internal/MinioClientConfig.java` to `src/main/java/com/thecodinglab/imdbclone/media/internal/ObjectStorageClientConfig.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/media/internal/MediaFiles.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/media/MediaServiceIntegrationTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java`

- [ ] **Step 1: Write the failing integration test changes**

In `MediaServiceIntegrationTest`, replace MinIO imports:

```java
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
```

with AWS imports:

```java
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
```

Replace:

```java
  @Autowired private MinioClient minioClient;
```

with:

```java
  @Autowired private S3Client s3Client;
```

Replace `assertObjectExists` with:

```java
  private void assertObjectExists(String objectName) {
    var stat =
        s3Client.headObject(
            HeadObjectRequest.builder()
                .bucket(storageProperties.bucketName())
                .key(objectName)
                .build());
    assertThat(stat.contentLength()).isPositive();
  }
```

Replace `assertObjectDoesNotExist` with:

```java
  private void assertObjectDoesNotExist(String objectName) {
    assertThatThrownBy(
            () ->
                s3Client.headObject(
                    HeadObjectRequest.builder()
                        .bucket(storageProperties.bucketName())
                        .key(objectName)
                        .build()))
        .isInstanceOf(S3Exception.class)
        .satisfies(
            ex -> {
              S3Exception s3Exception = (S3Exception) ex;
              assertThat(s3Exception.statusCode()).isEqualTo(404);
              assertThat(s3Exception.awsErrorDetails().errorCode())
                  .isIn("NoSuchKey", "NotFound", "404");
            });
  }
```

- [ ] **Step 2: Run the test and confirm it fails to compile**

Run:

```bash
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
```

Expected: compile fails because `software.amazon.awssdk` classes are not yet on the classpath.

- [ ] **Step 3: Add AWS SDK v2 dependencies and remove MinIO dependencies**

In `gradle.properties`, add:

```properties
awsSdkVersion=2.44.4
```

In `build.gradle`, add the AWS SDK BOM:

```gradle
dependencyManagement {
	imports {
		mavenBom 'org.springframework.modulith:spring-modulith-bom:2.0.6'
		mavenBom "software.amazon.awssdk:bom:${awsSdkVersion}"
	}
}
```

Replace the file storage dependency:

```gradle
	//-- File Storage
	implementation 'io.minio:minio:9.0.0'
```

with:

```gradle
	//-- File Storage
	implementation 'software.amazon.awssdk:s3'
	implementation 'software.amazon.awssdk:url-connection-client'
```

Remove the MinIO Testcontainers dependency:

```gradle
	testImplementation "org.testcontainers:testcontainers-minio:${testContainersVersion}"
```

- [ ] **Step 4: Replace the client config**

Rename `MinioClientConfig.java` to `ObjectStorageClientConfig.java` and replace the contents with:

```java
package com.thecodinglab.imdbclone.media.internal;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class ObjectStorageClientConfig {

  private static final Region SIGNING_REGION = Region.US_EAST_1;

  @Bean
  S3Client s3Client(MediaStorageProperties properties) {
    return S3Client.builder()
        .endpointOverride(URI.create(properties.uri()))
        .region(SIGNING_REGION)
        .credentialsProvider(credentialsProvider(properties))
        .forcePathStyle(true)
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }

  @Bean
  S3Presigner s3Presigner(MediaStorageProperties properties) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(properties.uri()))
        .region(SIGNING_REGION)
        .credentialsProvider(credentialsProvider(properties))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  private StaticCredentialsProvider credentialsProvider(MediaStorageProperties properties) {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
  }
}
```

- [ ] **Step 5: Replace `MediaFiles` MinIO fields and constructor args**

Replace imports:

```java
import io.minio.*;
import io.minio.Http.Method;
```

with:

```java
import java.time.Duration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
```

Replace fields:

```java
  private final MinioClient minioClient;
```

with:

```java
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
```

Replace constructor parameters and assignments:

```java
      MinioClient minioClient,
```

with:

```java
      S3Client s3Client,
      S3Presigner s3Presigner,
```

and:

```java
    this.minioClient = minioClient;
```

with:

```java
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
```

- [ ] **Step 6: Replace object write/delete/bucket/presign code**

Replace `storeFile` with:

```java
  private String storeFile(InputStream file, int fileSize, String fileName, String contentType) {
    try {
      PutObjectResponse response =
          s3Client.putObject(
              PutObjectRequest.builder()
                  .bucket(storageProperties.bucketName())
                  .contentType(contentType)
                  .key(fileName)
                  .build(),
              RequestBody.fromInputStream(file, fileSize));
      return "Image was stored with etag [" + response.eTag() + "]";
    } catch (Exception ex) {
      throw new ObjectStorageOperationException(
          "Error while storing file in object storage", ex);
    }
  }
```

Replace `deleteFile` with:

```java
  private void deleteFile(String imageName) {
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(storageProperties.bucketName())
              .key(imageName)
              .build());
    } catch (Exception ex) {
      throw new ObjectStorageOperationException(
          "Error while deleting file in object storage", ex);
    }
  }
```

Replace `setUpBucket` with:

```java
  void setUpBucket() {
    try {
      if (!bucketExists()) {
        s3Client.createBucket(
            CreateBucketRequest.builder().bucket(storageProperties.bucketName()).build());
      }
      String bucketPolicy = "config/object-storage-public-read-policy.json";
      createBucketPolicyFrom(bucketPolicy);
      logger.info(
          "bucket [{}] was created and bucketPolicy set successfully",
          storageProperties.bucketName());

    } catch (Exception ex) {
      logger.error("Creation of bucket [{}] failed", storageProperties.bucketName());
      throw new ObjectStorageOperationException(
          "Error while creating bucket in object storage", ex);
    }
  }
```

Add this helper below `setUpBucket`:

```java
  private boolean bucketExists() {
    try {
      s3Client.headBucket(
          HeadBucketRequest.builder().bucket(storageProperties.bucketName()).build());
      return true;
    } catch (NoSuchBucketException ex) {
      return false;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return false;
      }
      throw ex;
    }
  }
```

Replace `createBucketPolicyFrom` with:

```java
  private void createBucketPolicyFrom(String bucketPolicy) {
    String policyConfig = readResourceFile(bucketPolicy);
    try {
      s3Client.putBucketPolicy(
          PutBucketPolicyRequest.builder()
              .bucket(storageProperties.bucketName())
              .policy(policyConfig)
              .build());
    } catch (Exception ex) {
      logger.error("Creation of bucket policy failed");
      throw new ObjectStorageOperationException(
          "Error while creating bucket policy in object storage", ex);
    }
  }
```

Replace `generateUrl` with:

```java
  public String generateUrl(String imageName) {
    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder()
              .bucket(storageProperties.bucketName())
              .key(imageName)
              .build();
      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(Duration.ofDays(1))
              .getObjectRequest(getObjectRequest)
              .build();
      return s3Presigner.presignGetObject(presignRequest).url().toString();
    } catch (Exception ex) {
      logger.error("Generate presigned object URL file with image name [{}] failed", imageName);
      throw new ObjectStorageOperationException(
          "Error while generating presigned URL in object storage", ex);
    }
  }
```

- [ ] **Step 7: Run formatting and the narrow backend test**

Run:

```bash
./gradlew spotlessApply
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
```

Expected: formatting completes and the media integration test passes against RustFS through AWS SDK v2.

- [ ] **Step 8: Commit the AWS SDK v2 migration**

```bash
git add build.gradle gradle.properties src/main/java/com/thecodinglab/imdbclone/media/internal src/main/java/com/thecodinglab/imdbclone/shared/error src/test/java/com/thecodinglab/imdbclone/media/MediaServiceIntegrationTest.java src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java
git commit -m "refactor(storage): use AWS SDK S3 client"
```

### Task 4: Frontend Object Storage Naming

**Files:**
- Modify: `frontend/src/shared/media/imageUrls.ts`
- Modify: `frontend/src/shared/media/imageUrls.test.ts`
- Modify: `frontend/src/shared/media/index.ts`
- Modify: `frontend/src/vite-env.d.ts`
- Modify: `frontend/.env.production`
- Modify: `frontend/playwright.config.ts`

- [ ] **Step 1: Write frontend tests for new and legacy env vars**

Replace `frontend/src/shared/media/imageUrls.test.ts` with:

```ts
import {
  getMovieImageUrl,
  getObjectStorageImageUrl,
  getProfileImageUrl,
  ObjectStorageImageSize,
} from "./imageUrls";
import { vi } from "vitest";

describe("getObjectStorageImageUrl", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("builds a movie image URL from the configured object storage address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getObjectStorageImageUrl("poster-token", ObjectStorageImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_120x180.jpg",
    );
  });

  it("falls back to the legacy MinIO address during migration", () => {
    vi.stubEnv("VITE_IMDB_CLONE_MINIO_ADDRESS", "http://legacy-storage:9000");

    expect(getMovieImageUrl("poster-token", ObjectStorageImageSize.Large)).toBe(
      "http://legacy-storage:9000/imdb-clone/movies/poster-token_size_600x900.jpg",
    );
  });

  it("falls back to localhost when no object storage address is configured", () => {
    expect(getMovieImageUrl("poster-token", ObjectStorageImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_600x900.jpg",
    );
  });

  it("builds a profile image URL from the configured object storage address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getProfileImageUrl("avatar-token")).toBe(
      "http://localhost:9000/imdb-clone/profile-photos/avatar-token_size_800x800.jpg",
    );
  });
});
```

- [ ] **Step 2: Run the frontend test and confirm it fails**

Run:

```bash
cd frontend && yarn test -- imageUrls.test.ts
```

Expected: test fails because `ObjectStorageImageSize` and `getObjectStorageImageUrl` do not exist yet.

- [ ] **Step 3: Update `imageUrls.ts` with new names and temporary aliases**

Replace the file with:

```ts
export enum ObjectStorageImageSize {
  Small = "120x180",
  Large = "600x900",
  Profile = "800x800",
}

const getObjectStorageHost = () =>
  import.meta.env.VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS ??
  import.meta.env.VITE_IMDB_CLONE_MINIO_ADDRESS ??
  "http://localhost:9000";

export type MovieImageSize =
  | ObjectStorageImageSize.Small
  | ObjectStorageImageSize.Large;

export const getMovieImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  const objectStorageHost = getObjectStorageHost();
  const basePath = "/imdb-clone/movies/";

  return `${objectStorageHost}${basePath}${token}_size_${size}.jpg`;
};

export const getProfileImageUrl = (token: string): string => {
  const objectStorageHost = getObjectStorageHost();
  const basePath = "/imdb-clone/profile-photos/";

  return `${objectStorageHost}${basePath}${token}_size_${ObjectStorageImageSize.Profile}.jpg`;
};

export const getObjectStorageImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  return getMovieImageUrl(token, size);
};

export const MinioImageSize = ObjectStorageImageSize;
export const getMinioImageUrl = getObjectStorageImageUrl;
```

- [ ] **Step 4: Update frontend exports and env types**

In `frontend/src/shared/media/index.ts`, replace the export block with:

```ts
export {
  getMinioImageUrl,
  getMovieImageUrl,
  getObjectStorageImageUrl,
  getProfileImageUrl,
  MinioImageSize,
  ObjectStorageImageSize,
} from "./imageUrls";
export type { MovieImageSize } from "./imageUrls";
```

In `frontend/src/vite-env.d.ts`, add the new env var:

```ts
  readonly VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS?: string;
```

Keep `VITE_IMDB_CLONE_MINIO_ADDRESS` during the transition.

In `frontend/playwright.config.ts`, replace:

```ts
      VITE_IMDB_CLONE_MINIO_ADDRESS: "http://localhost:9000",
```

with:

```ts
      VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS: "http://localhost:9000",
```

- [ ] **Step 5: Update production build env**

In `frontend/.env.production`, replace:

```properties
REACT_APP_IMDB_CLONE_MINIO_ADDRESS=https://minio.imdb-clone.the-coding-lab.com
```

with:

```properties
VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS=https://rustfs.imdb-clone.the-coding-lab.com
```

Keep the backend env untouched in this task unless the app already has a separate production env fix in progress.

- [ ] **Step 6: Run frontend media tests**

Run:

```bash
cd frontend && yarn test -- imageUrls.test.ts
```

Expected: the image URL tests pass.

- [ ] **Step 7: Commit frontend object storage naming**

```bash
git add frontend/src/shared/media/imageUrls.ts frontend/src/shared/media/imageUrls.test.ts frontend/src/shared/media/index.ts frontend/src/vite-env.d.ts frontend/.env.production frontend/playwright.config.ts
git commit -m "refactor(frontend): rename storage image env"
```

### Task 5: Deployment and Seed Migration

**Files:**
- Modify: `infrastructure/deployment/development/docker-compose.yaml`
- Modify: `infrastructure/deployment/production/docker-compose.stateful-apps.yaml`
- Modify: `infrastructure/deployment/production/docker-compose.stateless-apps.yaml`
- Modify: `infrastructure/deployment/production/generate_credentials.sh`
- Modify: `infrastructure/minio/dev-seed/upload_to_minio.sh`
- Modify: `Makefile`

- [ ] **Step 1: Update development and production stateful compose services**

In both stateful compose files, replace the MinIO service name with `imdb-clone-rustfs`, use image `rustfs/rustfs:latest`, set:

```yaml
      - RUSTFS_ACCESS_KEY=ROOTNAME
      - RUSTFS_SECRET_KEY=CHANGEME123
      - RUSTFS_CONSOLE_ENABLE=true
```

for development, and:

```yaml
      - RUSTFS_ACCESS_KEY=${OBJECT_STORAGE_ACCESS_KEY_ENV_VAR}
      - RUSTFS_SECRET_KEY=${OBJECT_STORAGE_SECRET_KEY_ENV_VAR}
      - RUSTFS_CONSOLE_ENABLE=true
```

for production.

Expose ports:

```yaml
      - "9000:9000"
      - "9001:9001"
```

Use volume:

```yaml
      - imdb-clone-rustfs-data:/data
```

and command:

```yaml
    command: /data
```

- [ ] **Step 2: Update stateful seed jobs**

In seed jobs, replace alias setup with:

```sh
mc alias set rustfs http://imdb-clone-rustfs:9000 $${OBJECT_STORAGE_ACCESS_KEY} $${OBJECT_STORAGE_SECRET_KEY} &&
(mc ls rustfs | grep "imdb-clone") || mc mb rustfs/imdb-clone &&
mc cp --recursive /tmp/movies rustfs/imdb-clone &&
```

For development, set `OBJECT_STORAGE_ACCESS_KEY=ROOTNAME` and `OBJECT_STORAGE_SECRET_KEY=CHANGEME123`.

For production, map those from `${OBJECT_STORAGE_ACCESS_KEY_ENV_VAR}` and `${OBJECT_STORAGE_SECRET_KEY_ENV_VAR}`.

- [ ] **Step 3: Update stateless production storage references**

In `docker-compose.stateless-apps.yaml`, replace:

```yaml
      - IMDB_CLONE_MEDIA_STORAGE_URI=http://imdb-clone-minio:9000
      - IMDB_CLONE_MEDIA_STORAGE_ACCESS_KEY=${MINIO_ACCESS_KEY_ENV_VAR}
      - IMDB_CLONE_MEDIA_STORAGE_SECRET_KEY=${MINIO_SECRET_KEY_ENV_VAR}
```

with:

```yaml
      - IMDB_CLONE_MEDIA_STORAGE_URI=http://imdb-clone-rustfs:9000
      - IMDB_CLONE_MEDIA_STORAGE_ACCESS_KEY=${OBJECT_STORAGE_ACCESS_KEY_ENV_VAR}
      - IMDB_CLONE_MEDIA_STORAGE_SECRET_KEY=${OBJECT_STORAGE_SECRET_KEY_ENV_VAR}
```

Replace:

```yaml
      - REACT_APP_IMDB_CLONE_MINIO_ADDRESS=${MINIO_ADDRESS_ENV_VAR}
```

with:

```yaml
      - VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS=${OBJECT_STORAGE_ADDRESS_ENV_VAR}
```

This variable is useful for local compose documentation, but the production Docker image is built from `frontend/.env.production`. Keep both places consistent.

- [ ] **Step 4: Update credential generation names**

In `generate_credentials.sh`, replace generated MinIO names with:

```sh
OBJECT_STORAGE_ACCESS_KEY_ENV_VAR=$(generate_password 12)
OBJECT_STORAGE_SECRET_KEY_ENV_VAR=$(generate_password 16)
OBJECT_STORAGE_ADDRESS_ENV_VAR=https://rustfs.imdb-clone.the-coding-lab.com
```

Update the output block to print those three names.

- [ ] **Step 5: Update local dev seed script env names**

In `upload_to_minio.sh`, keep the file name for this task and replace default variables with:

```bash
OBJECT_STORAGE_ALIAS="${OBJECT_STORAGE_ALIAS:-imdb-clone-local}"
OBJECT_STORAGE_ENDPOINT="${OBJECT_STORAGE_ENDPOINT:-http://localhost:9000}"
OBJECT_STORAGE_ACCESS_KEY="${OBJECT_STORAGE_ACCESS_KEY:-ROOTNAME}"
OBJECT_STORAGE_SECRET_KEY="${OBJECT_STORAGE_SECRET_KEY:-CHANGEME123}"
OBJECT_STORAGE_BUCKET="${OBJECT_STORAGE_BUCKET:-imdb-clone}"
DOCKER_OBJECT_STORAGE_ENDPOINT="${DOCKER_OBJECT_STORAGE_ENDPOINT:-http://imdb-clone-rustfs:9000}"
```

Update all `mc` commands to use `"$OBJECT_STORAGE_ALIAS"` and the `rustfs` alias inside Docker.

- [ ] **Step 6: Update Makefile target names and descriptions**

Replace:

```make
seed-minio-dev-movie-images:
```

with:

```make
seed-object-storage-dev-movie-images:
```

Keep a compatibility alias:

```make
seed-minio-dev-movie-images: seed-object-storage-dev-movie-images
```

- [ ] **Step 7: Run syntax checks for changed shell files**

Run:

```bash
bash -n infrastructure/deployment/production/generate_credentials.sh
bash -n infrastructure/minio/dev-seed/upload_to_minio.sh
```

Expected: both commands exit with code `0`.

- [ ] **Step 8: Commit deployment and seed migration**

```bash
git add infrastructure/deployment/development/docker-compose.yaml infrastructure/deployment/production/docker-compose.stateful-apps.yaml infrastructure/deployment/production/docker-compose.stateless-apps.yaml infrastructure/deployment/production/generate_credentials.sh infrastructure/minio/dev-seed/upload_to_minio.sh Makefile
git commit -m "chore(storage): deploy RustFS runtime"
```

### Task 6: Documentation and Final Verification

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `infrastructure/README.md`
- Modify: `infrastructure/deployment/README.md`
- Modify: targeted files under `infrastructure/minio`

- [ ] **Step 1: Update docs from MinIO to RustFS/object storage**

Replace runtime descriptions with:

```text
File Storage: RustFS S3-compatible object storage
```

For project architecture text, prefer:

```text
S3-compatible object storage
```

Keep `mc` references as "MinIO Client (`mc`)" because the CLI name remains `mc`.

- [ ] **Step 2: Search for remaining MinIO references**

Run:

```bash
rg -n "MinIO|Minio|MINIO|imdb-clone-minio|VITE_IMDB_CLONE_MINIO_ADDRESS|REACT_APP_IMDB_CLONE_MINIO_ADDRESS|io\\.minio|testcontainers-minio" .
```

Expected remaining references are limited to:

```text
frontend/src/shared/media/imageUrls.ts
frontend/src/shared/media/index.ts
frontend/src/vite-env.d.ts
frontend/src/shared/media/imageUrls.test.ts
docs/superpowers/specs/2026-05-16-rustfs-migration-design.md
docs/superpowers/plans/2026-05-16-rustfs-aws-sdk-migration.md
```

The frontend references are temporary compatibility aliases.

- [ ] **Step 3: Run backend formatting and tests**

Run:

```bash
./gradlew spotlessApply
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
./gradlew test
```

Expected: all commands pass.

- [ ] **Step 4: Run frontend tests for storage helper**

Run:

```bash
cd frontend && yarn test -- imageUrls.test.ts
```

Expected: the image URL tests pass.

- [ ] **Step 5: Run broader checks**

Run:

```bash
./gradlew build jacocoTestReport
cd frontend && yarn run lint
cd frontend && yarn build
```

Expected: all commands pass.

- [ ] **Step 6: Commit documentation updates**

```bash
git add README.md AGENTS.md infrastructure/README.md infrastructure/deployment/README.md infrastructure/minio docs/superpowers/plans/2026-05-16-rustfs-aws-sdk-migration.md
git commit -m "docs(storage): document RustFS migration"
```
