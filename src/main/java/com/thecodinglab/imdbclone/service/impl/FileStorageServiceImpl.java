package com.thecodinglab.imdbclone.service.impl;

import com.thecodinglab.imdbclone.config.MinioClientConfig;
import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.FileStorageService;
import com.thecodinglab.imdbclone.service.MovieService;
import com.thecodinglab.imdbclone.utility.Utility;
import com.thecodinglab.imdbclone.utility.images.Image;
import com.thecodinglab.imdbclone.utility.images.MovieImageConstants;
import com.thecodinglab.imdbclone.utility.images.ProfilePhotoConstants;
import com.thecodinglab.imdbclone.validation.ImageSize;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageServiceImpl.class);

  @Value("${minio.rest.bucketName}")
  public String bucketName;

  private final MinioClient minioClient;
  private final MovieRepository movieRepository;
  private final AccountRepository accountRepository;
  private final MovieService movieService;

  public FileStorageServiceImpl(
      MinioClientConfig minioClient,
      MovieRepository movieRepository,
      AccountRepository accountRepository,
      MovieService movieService) {
    this.minioClient = minioClient.getClient();
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
    this.movieService = movieService;
  }

  @Override
  @Transactional
  public List<String> storeProfilePhoto(MultipartFile file, UserPrincipal currentUser) {
    // validation
    //    ImageSize.validateProfilePhoto(file);

    // persist image-url-token in database
    Account account = accountRepository.getAccount(currentUser);
    String imageUrlToken;

    if (account.getImageUrlToken() == null) {
      imageUrlToken = Utility.generateToken();
    } else {
      imageUrlToken = account.getImageUrlToken();
    }

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

    account.setImageUrlToken(imageUrlToken);
    accountRepository.save(account);

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

    Account account = accountRepository.getAccount(currentUser);

    deleteFile(ProfilePhotoConstants.getDetailViewImageName(account.getImageUrlToken()));
    deleteFile(ProfilePhotoConstants.getThumbnailImageName(account.getImageUrlToken()));

    return "Profile Photos of User with accountId ["
        + account.getId()
        + "] and imageUrlToken ["
        + account.getImageUrlToken()
        + "] were deleted";
  }

  @Override
  @Transactional
  public List<String> storeMovieImage(MultipartFile file, Long movieId) {

    // validation
    Movie movie = movieRepository.getMovieById(movieId);
    ImageSize.validateMovieImage(file);

    String imageUrlToken;
    if (movie.getImageUrlToken() == null) {
      imageUrlToken = Utility.generateToken();
    } else {
      imageUrlToken = movie.getImageUrlToken();
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
    movie.setImageUrlToken(imageUrlToken);
    movieService.performSave(movie);

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
    Movie movie = movieRepository.getMovieById(movieId);

    // delete images
    deleteFile(MovieImageConstants.getDetailViewImageName(movie.getImageUrlToken()));
    deleteFile(MovieImageConstants.getThumbNailImageName(movie.getImageUrlToken()));

    return "Movie images of movie with movieId [" + movie.getId() + "] were deleted";
  }

  @Override
  public String storeFile(InputStream file, int fileSize, String fileName, String contentType) {
    try {
      ObjectWriteResponse resp =
          minioClient.putObject(
              PutObjectArgs.builder()
                  .bucket(bucketName)
                  .contentType(contentType)
                  .object(fileName)
                  .stream(file, fileSize, -1)
                  .build());
      return "Image was stored with etag [" + resp.etag() + "]";
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteFile(String imageName) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(imageName).build());
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setUpBucket() {
    try {
      if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
        // create bucket
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      }
      // set policy
      String bucketPolicy = "config/minio-policy.json";
      createBucketPolicyFrom(bucketPolicy);
      LOGGER.info("bucket [{}] was created and bucketPolicy set successfully", bucketName);

    } catch (Exception e) {
      LOGGER.error("Creation of bucket [{}] failed", bucketName);
      throw new RuntimeException(e);
    }
  }

  private void createBucketPolicyFrom(String bucketPolicy) {
    String policyConfig = readResourceFile(bucketPolicy);
    try {
      minioClient.setBucketPolicy(
          SetBucketPolicyArgs.builder().bucket(bucketName).config(policyConfig).build());
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }
  }

  private String readResourceFile(String resourcePath) {
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
      if (inputStream == null) {
        throw new FileNotFoundException("File not found: " + resourcePath);
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
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException
        | ServerException e) {
      throw new RuntimeException(e);
    }
  }
}
