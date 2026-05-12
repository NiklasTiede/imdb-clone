package com.thecodinglab.imdbclone.media.internal;

import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.account.api.AccountImageToken;
import com.thecodinglab.imdbclone.catalog.api.MovieImageToken;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.config.MinioClientConfig;
import com.thecodinglab.imdbclone.exception.domain.MinioOperationException;
import com.thecodinglab.imdbclone.media.api.MediaService;
import com.thecodinglab.imdbclone.media.internal.images.Image;
import com.thecodinglab.imdbclone.media.internal.images.ImageSize;
import com.thecodinglab.imdbclone.media.internal.images.MovieImageConstants;
import com.thecodinglab.imdbclone.media.internal.images.ProfilePhotoConstants;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import io.minio.*;
import io.minio.Http.Method;
import jakarta.transaction.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaServiceImpl implements MediaService {

  private static final Logger logger = LoggerFactory.getLogger(MediaServiceImpl.class);

  @Value("${minio.rest.bucket-name}")
  public String bucketName;

  private final MinioClient minioClient;
  private final AccountImageService accountImageService;
  private final MovieService movieService;

  public MediaServiceImpl(
      MinioClientConfig minioClient,
      AccountImageService accountImageService,
      MovieService movieService) {
    this.minioClient = minioClient.getClient();
    this.accountImageService = accountImageService;
    this.movieService = movieService;
  }

  @Override
  @Transactional
  public List<String> storeProfilePhoto(MultipartFile file, UserPrincipal currentUser) {
    // validation
    //    ImageSize.validateProfilePhoto(file);

    // persist image-url-token in database
    AccountImageToken accountImageToken = accountImageService.getProfileImageToken(currentUser);

    String imageUrlToken =
        (accountImageToken.imageUrlToken() != null)
            ? accountImageToken.imageUrlToken()
            : Image.generateToken();

    // better: create new token, each time new image is created. token change is used for
    // rerendering!

    // generate 2 photos of size 800x800 and 120x120
    List<Image> profilePhotos =
        Image.createImages(
            file,
            ProfilePhotoConstants.TARGET_SIZES,
            ProfilePhotoConstants.ASPECT_RATIO,
            ProfilePhotoConstants.BUCKET_DIRECTORY_NAME,
            imageUrlToken);

    accountImageService.updateProfileImageToken(accountImageToken.accountId(), imageUrlToken);

    // store photos in bucket
    return profilePhotos.stream()
        .map(
            photo ->
                storeFile(
                    photo.getInputStream(),
                    photo.getStreamSize(),
                    photo.getImageName(),
                    photo.getContentType()))
        .toList();
  }

  @Override
  public String deleteProfilePhoto(UserPrincipal currentUser) {

    AccountImageToken accountImageToken = accountImageService.getProfileImageToken(currentUser);

    deleteFile(ProfilePhotoConstants.getDetailViewImageName(accountImageToken.imageUrlToken()));
    deleteFile(ProfilePhotoConstants.getThumbnailImageName(accountImageToken.imageUrlToken()));

    return "Profile Photos of User with accountId [%d] and imageUrlToken [%s] were deleted"
        .formatted(accountImageToken.accountId(), accountImageToken.imageUrlToken());
  }

  @Override
  @Transactional
  public List<String> storeMovieImage(MultipartFile file, Long movieId) {

    // validation
    MovieImageToken movieImageToken = movieService.getMovieImageToken(movieId);
    ImageSize.validateMovieImage(file);

    String imageUrlToken;
    if (movieImageToken.imageUrlToken() == null) {
      imageUrlToken = Image.generateToken();
    } else {
      imageUrlToken = movieImageToken.imageUrlToken();
    }

    // generate 2 images of size 600x900 and 120x180
    List<Image> movieImages =
        Image.createImages(
            file,
            MovieImageConstants.TARGET_SIZES,
            MovieImageConstants.ASPECT_RATIO,
            MovieImageConstants.BUCKET_DIRECTORY_NAME,
            imageUrlToken);

    // save in DB and ES
    movieService.updateMovieImageToken(movieId, imageUrlToken);

    // store images
    return movieImages.stream()
        .map(
            image ->
                storeFile(
                    image.getInputStream(),
                    image.getStreamSize(),
                    image.getImageName(),
                    image.getContentType()))
        .toList();
  }

  @Override
  public String deleteMovieImage(Long movieId) {
    // find / validate movie
    MovieImageToken movieImageToken = movieService.getMovieImageToken(movieId);

    // delete images
    deleteFile(MovieImageConstants.getDetailViewImageName(movieImageToken.imageUrlToken()));
    deleteFile(MovieImageConstants.getThumbNailImageName(movieImageToken.imageUrlToken()));

    return "Movie images of movie with movieId [%d] were deleted"
        .formatted(movieImageToken.movieId());
  }

  private String storeFile(InputStream file, int fileSize, String fileName, String contentType) {
    try {
      ObjectWriteResponse resp =
          minioClient.putObject(
              PutObjectArgs.builder()
                  .bucket(bucketName)
                  .contentType(contentType)
                  .object(fileName)
                  .stream(file, (long) fileSize, -1L)
                  .build());
      return "Image was stored with etag [" + resp.etag() + "]";
    } catch (Exception ex) {
      throw new MinioOperationException("Error while storing file in MinIO", ex);
    }
  }

  private void deleteFile(String imageName) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(imageName).build());
    } catch (Exception ex) {
      throw new MinioOperationException("Error while deleting file in MinIO", ex);
    }
  }

  void setUpBucket() {
    try {
      if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
        // create bucket
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      }
      // set policy
      String bucketPolicy = "config/minio-policy.json";
      createBucketPolicyFrom(bucketPolicy);
      logger.info("bucket [{}] was created and bucketPolicy set successfully", bucketName);

    } catch (Exception ex) {
      logger.error("Creation of bucket [{}] failed", bucketName);
      throw new MinioOperationException("Error while creating bucket in MinIO", ex);
    }
  }

  private void createBucketPolicyFrom(String bucketPolicy) {
    String policyConfig = readResourceFile(bucketPolicy);
    try {
      minioClient.setBucketPolicy(
          SetBucketPolicyArgs.builder().bucket(bucketName).config(policyConfig).build());
    } catch (Exception ex) {
      logger.error("Creation of bucket policy failed");
      throw new MinioOperationException("Error while creating bucket policy in MinIO", ex);
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
      throw new MinioOperationException("Error while reading policy config", ex);
    }
  }

  public String generateUrl(String imageName) {
    String presignedUrl;

    try {
      presignedUrl =
          minioClient.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .bucket(bucketName)
                  .method(Method.GET)
                  .object(imageName)
                  .expiry(60 * 60 * 24)
                  .build());
      return presignedUrl;
    } catch (Exception ex) {
      logger.error("Generate presigned object URL file with image name [{}] failed", imageName);
      throw new MinioOperationException("Error while generating presigned URL in MinIO", ex);
    }
  }
}
