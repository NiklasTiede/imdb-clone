package com.thecodinglab.imdbclone.media.internal;

import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.account.api.AccountImageToken;
import com.thecodinglab.imdbclone.account.api.events.AccountDeleted;
import com.thecodinglab.imdbclone.catalog.api.MovieImageService;
import com.thecodinglab.imdbclone.catalog.api.MovieImageToken;
import com.thecodinglab.imdbclone.catalog.api.events.MovieDeleted;
import com.thecodinglab.imdbclone.media.internal.images.Image;
import com.thecodinglab.imdbclone.media.internal.images.ImageSize;
import com.thecodinglab.imdbclone.media.internal.images.MovieImageConstants;
import com.thecodinglab.imdbclone.media.internal.images.ProfilePhotoConstants;
import com.thecodinglab.imdbclone.shared.error.ObjectStorageOperationException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.minio.*;
import io.minio.Http.Method;
import jakarta.transaction.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaFiles implements MediaService {

  private static final Logger logger = LoggerFactory.getLogger(MediaFiles.class);

  private final MinioClient minioClient;
  private final MediaStorageProperties storageProperties;
  private final AccountImageService accountImageService;
  private final MovieImageService movieImageService;

  public MediaFiles(
      MinioClient minioClient,
      MediaStorageProperties storageProperties,
      AccountImageService accountImageService,
      MovieImageService movieImageService) {
    this.minioClient = minioClient;
    this.storageProperties = storageProperties;
    this.accountImageService = accountImageService;
    this.movieImageService = movieImageService;
  }

  @Override
  @Transactional
  public List<String> storeProfilePhoto(MultipartFile file, UserPrincipal currentUser) {
    ImageSize.validateProfilePhoto(file);
    AccountImageToken accountImageToken = accountImageService.getProfileImageToken(currentUser);
    String oldImageUrlToken = accountImageToken.imageUrlToken();
    String imageUrlToken = Image.generateToken();

    List<Image> profilePhotos =
        Image.createImages(
            file,
            ProfilePhotoConstants.TARGET_SIZES,
            ProfilePhotoConstants.ASPECT_RATIO,
            ProfilePhotoConstants.BUCKET_DIRECTORY_NAME,
            imageUrlToken);

    List<String> storedImages = storeFiles(profilePhotos);
    accountImageService.updateProfileImageToken(accountImageToken.accountId(), imageUrlToken);
    deleteProfilePhotoObjectsBestEffort(oldImageUrlToken);
    return storedImages;
  }

  @Override
  public String deleteProfilePhoto(UserPrincipal currentUser) {

    AccountImageToken accountImageToken = accountImageService.getProfileImageToken(currentUser);
    if (accountImageToken.imageUrlToken() == null) {
      return "No profile photo of User with accountId [%d] exists"
          .formatted(accountImageToken.accountId());
    }

    deleteProfilePhotoObjects(accountImageToken.imageUrlToken());
    accountImageService.clearProfileImageToken(accountImageToken.accountId());

    return "Profile Photos of User with accountId [%d] and imageUrlToken [%s] were deleted"
        .formatted(accountImageToken.accountId(), accountImageToken.imageUrlToken());
  }

  @Override
  @Transactional
  public List<String> storeMovieImage(MultipartFile file, Long movieId) {

    MovieImageToken movieImageToken = movieImageService.getMovieImageToken(movieId);
    ImageSize.validateMovieImage(file);
    String oldImageUrlToken = movieImageToken.imageUrlToken();
    String imageUrlToken = Image.generateToken();

    List<Image> movieImages =
        Image.createImages(
            file,
            MovieImageConstants.TARGET_SIZES,
            MovieImageConstants.ASPECT_RATIO,
            MovieImageConstants.BUCKET_DIRECTORY_NAME,
            imageUrlToken);

    List<String> storedImages = storeFiles(movieImages);
    movieImageService.updateMovieImageToken(movieId, imageUrlToken);
    deleteMovieImageObjectsBestEffort(oldImageUrlToken);
    return storedImages;
  }

  @Override
  public String deleteMovieImage(Long movieId) {
    MovieImageToken movieImageToken = movieImageService.getMovieImageToken(movieId);
    if (movieImageToken.imageUrlToken() == null) {
      return "No movie image of movie with movieId [%d] exists"
          .formatted(movieImageToken.movieId());
    }

    deleteMovieImageObjects(movieImageToken.imageUrlToken());
    movieImageService.clearMovieImageToken(movieImageToken.movieId());

    return "Movie images of movie with movieId [%d] were deleted"
        .formatted(movieImageToken.movieId());
  }

  @TransactionalEventListener
  public void on(AccountDeleted event) {
    deleteProfilePhotoObjectsBestEffort(event.imageUrlToken());
  }

  @TransactionalEventListener
  public void on(MovieDeleted event) {
    deleteMovieImageObjectsBestEffort(event.imageUrlToken());
  }

  private List<String> storeFiles(List<Image> images) {
    return images.stream()
        .map(
            image ->
                storeFile(
                    image.getInputStream(),
                    image.getStreamSize(),
                    image.getImageName(),
                    image.getContentType()))
        .toList();
  }

  private void deleteProfilePhotoObjects(String imageUrlToken) {
    if (imageUrlToken == null) {
      return;
    }
    deleteFile(ProfilePhotoConstants.getDetailViewImageName(imageUrlToken));
    deleteFile(ProfilePhotoConstants.getThumbnailImageName(imageUrlToken));
  }

  private void deleteMovieImageObjects(String imageUrlToken) {
    if (imageUrlToken == null) {
      return;
    }
    deleteFile(MovieImageConstants.getDetailViewImageName(imageUrlToken));
    deleteFile(MovieImageConstants.getThumbNailImageName(imageUrlToken));
  }

  private void deleteProfilePhotoObjectsBestEffort(String imageUrlToken) {
    try {
      deleteProfilePhotoObjects(imageUrlToken);
    } catch (ObjectStorageOperationException ex) {
      logger.warn("Could not delete previous profile photo objects for token [{}]", imageUrlToken);
    }
  }

  private void deleteMovieImageObjectsBestEffort(String imageUrlToken) {
    try {
      deleteMovieImageObjects(imageUrlToken);
    } catch (ObjectStorageOperationException ex) {
      logger.warn("Could not delete previous movie image objects for token [{}]", imageUrlToken);
    }
  }

  private String storeFile(InputStream file, int fileSize, String fileName, String contentType) {
    try {
      ObjectWriteResponse resp =
          minioClient.putObject(
              PutObjectArgs.builder()
                  .bucket(storageProperties.bucketName())
                  .contentType(contentType)
                  .object(fileName)
                  .stream(file, (long) fileSize, -1L)
                  .build());
      return "Image was stored with etag [" + resp.etag() + "]";
    } catch (Exception ex) {
      throw new ObjectStorageOperationException("Error while storing file in object storage", ex);
    }
  }

  private void deleteFile(String imageName) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(storageProperties.bucketName())
              .object(imageName)
              .build());
    } catch (Exception ex) {
      throw new ObjectStorageOperationException("Error while deleting file in object storage", ex);
    }
  }

  void setUpBucket() {
    try {
      if (!minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(storageProperties.bucketName()).build())) {
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket(storageProperties.bucketName()).build());
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

  private void createBucketPolicyFrom(String bucketPolicy) {
    String policyConfig = readResourceFile(bucketPolicy);
    try {
      minioClient.setBucketPolicy(
          SetBucketPolicyArgs.builder()
              .bucket(storageProperties.bucketName())
              .config(policyConfig)
              .build());
    } catch (Exception ex) {
      logger.error("Creation of bucket policy failed");
      throw new ObjectStorageOperationException(
          "Error while creating bucket policy in object storage", ex);
    }
  }

  private String readResourceFile(String resourcePath) {
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
      if (inputStream == null) {
        throw new FileNotFoundException("File not found: %s".formatted(resourcePath));
      }

      try (BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          stringBuilder.append(line);
        }
        return stringBuilder.toString();
      }
    } catch (IOException ex) {
      logger.error("Reading file with path [{}] failed", resourcePath);
      throw new ObjectStorageOperationException("Error while reading policy config", ex);
    }
  }

  public String generateUrl(String imageName) {
    String presignedUrl;

    try {
      presignedUrl =
          minioClient.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .bucket(storageProperties.bucketName())
                  .method(Method.GET)
                  .object(imageName)
                  .expiry(60 * 60 * 24)
                  .build());
      return presignedUrl;
    } catch (Exception ex) {
      logger.error("Generate presigned object URL file with image name [{}] failed", imageName);
      throw new ObjectStorageOperationException(
          "Error while generating presigned URL in object storage", ex);
    }
  }
}
