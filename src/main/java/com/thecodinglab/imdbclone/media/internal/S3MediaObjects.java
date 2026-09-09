package com.thecodinglab.imdbclone.media.internal;

import com.thecodinglab.imdbclone.media.internal.images.Image;
import com.thecodinglab.imdbclone.shared.error.ObjectStorageOperationException;
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
class S3MediaObjects implements MediaObjects {
  private final S3Client client;
  private final S3Presigner presigner;
  private final MediaStorageProperties properties;

  S3MediaObjects(S3Client client, S3Presigner presigner, MediaStorageProperties properties) {
    this.client = client;
    this.presigner = presigner;
    this.properties = properties;
  }

  @Override
  public String store(Image image) {
    try {
      var response =
          client.putObject(
              PutObjectRequest.builder()
                  .bucket(properties.bucketName())
                  .key(image.getImageName())
                  .contentType(image.getContentType())
                  .build(),
              RequestBody.fromInputStream(image.getInputStream(), image.getStreamSize()));
      return "Image was stored with etag [" + response.eTag() + "]";
    } catch (Exception exception) {
      throw new ObjectStorageOperationException(
          "Error while storing file in object storage", exception);
    }
  }

  @Override
  public void delete(MediaKind kind, String token) {
    try {
      for (String key : kind.objectNames(token)) {
        client.deleteObject(
            DeleteObjectRequest.builder().bucket(properties.bucketName()).key(key).build());
      }
    } catch (Exception exception) {
      throw new ObjectStorageOperationException(
          "Error while deleting file in object storage", exception);
    }
  }

  @Override
  public String generateUrl(String imageName) {
    try {
      var request =
          GetObjectRequest.builder().bucket(properties.bucketName()).key(imageName).build();
      return presigner
          .presignGetObject(
              GetObjectPresignRequest.builder()
                  .signatureDuration(Duration.ofDays(1))
                  .getObjectRequest(request)
                  .build())
          .url()
          .toString();
    } catch (Exception exception) {
      throw new ObjectStorageOperationException(
          "Error while generating presigned URL in object storage", exception);
    }
  }
}
