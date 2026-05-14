package com.thecodinglab.imdbclone.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.media.internal.MediaService;
import com.thecodinglab.imdbclone.media.internal.MediaStorageProperties;
import com.thecodinglab.imdbclone.media.internal.images.MovieImageConstants;
import com.thecodinglab.imdbclone.media.internal.images.ProfilePhotoConstants;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.support.BaseContainers;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@SpringBootTest
class MediaServiceIntegrationTest extends BaseContainers {

  private static final Path MOVIE_IMAGE =
      Path.of("src/main/resources/api-calls/minio/raw-movie-image.jpg");
  private static final Path PROFILE_PHOTO =
      Path.of("src/main/resources/api-calls/minio/raw-profile-photo.jpeg");

  @Autowired private MediaService mediaService;

  @Autowired private MovieRepository movieRepository;

  @Autowired private AccountRepository accountRepository;

  @Autowired private MinioClient minioClient;

  @Autowired private MediaStorageProperties storageProperties;

  @Test
  void storeMovieImage_updatesMovieTokenAndStoresExpectedObjects() throws Exception {
    var upload = imageUpload("image", "raw-movie-image.jpg", MOVIE_IMAGE);

    List<String> storedImages = mediaService.storeMovieImage(upload, 1L);

    var movie = movieRepository.getMovieById(1L);
    assertThat(movie.getImageUrlToken()).isNotBlank();
    assertThat(storedImages).hasSize(2);
    assertObjectExists(MovieImageConstants.getDetailViewImageName(movie.getImageUrlToken()));
    assertObjectExists(MovieImageConstants.getThumbNailImageName(movie.getImageUrlToken()));
  }

  @Test
  void deleteMovieImage_clearsMovieTokenAndDeletesObjects() throws Exception {
    var upload = imageUpload("image", "raw-movie-image.jpg", MOVIE_IMAGE);
    mediaService.storeMovieImage(upload, 1L);

    String imageUrlToken = movieRepository.getMovieById(1L).getImageUrlToken();
    String detailImageName = MovieImageConstants.getDetailViewImageName(imageUrlToken);
    String thumbnailImageName = MovieImageConstants.getThumbNailImageName(imageUrlToken);

    mediaService.deleteMovieImage(1L);

    assertObjectDoesNotExist(detailImageName);
    assertObjectDoesNotExist(thumbnailImageName);
    assertThat(movieRepository.getMovieById(1L).getImageUrlToken()).isNull();
  }

  @Test
  void storeProfilePhoto_updatesAccountTokenAndStoresExpectedObjects() throws Exception {
    var upload = imageUpload("image", "raw-profile-photo.jpeg", PROFILE_PHOTO);
    var currentUser =
        new UserPrincipal(
            2L,
            null,
            null,
            "test_user_two",
            "two@web.com",
            "password",
            false,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    List<String> storedImages = mediaService.storeProfilePhoto(upload, currentUser);

    var account = accountRepository.getAccountByUsername("test_user_two");
    assertThat(account.getImageUrlToken()).isNotBlank();
    assertThat(storedImages).hasSize(2);
    assertObjectExists(ProfilePhotoConstants.getDetailViewImageName(account.getImageUrlToken()));
    assertObjectExists(ProfilePhotoConstants.getThumbnailImageName(account.getImageUrlToken()));
  }

  @Test
  void deleteProfilePhoto_clearsAccountTokenAndDeletesObjects() throws Exception {
    var upload = imageUpload("image", "raw-profile-photo.jpeg", PROFILE_PHOTO);
    var currentUser =
        new UserPrincipal(
            2L,
            null,
            null,
            "test_user_two",
            "two@web.com",
            "password",
            false,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
    mediaService.storeProfilePhoto(upload, currentUser);

    String imageUrlToken =
        accountRepository.getAccountByUsername("test_user_two").getImageUrlToken();
    String detailImageName = ProfilePhotoConstants.getDetailViewImageName(imageUrlToken);
    String thumbnailImageName = ProfilePhotoConstants.getThumbnailImageName(imageUrlToken);

    mediaService.deleteProfilePhoto(currentUser);

    assertObjectDoesNotExist(detailImageName);
    assertObjectDoesNotExist(thumbnailImageName);
    assertThat(accountRepository.getAccountByUsername("test_user_two").getImageUrlToken())
        .isNull();
  }

  private MockMultipartFile imageUpload(String name, String fileName, Path image)
      throws IOException {
    return new MockMultipartFile(name, fileName, "image/jpeg", Files.newInputStream(image));
  }

  private void assertObjectExists(String objectName) throws Exception {
    var stat =
        minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(storageProperties.bucketName())
                .object(objectName)
                .build());
    assertThat(stat.object()).isEqualTo(objectName);
    assertThat(stat.size()).isPositive();
  }

  private void assertObjectDoesNotExist(String objectName) {
    assertThatThrownBy(
            () ->
                minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(storageProperties.bucketName())
                        .object(objectName)
                        .build()))
        .isInstanceOf(ErrorResponseException.class)
        .satisfies(
            ex ->
                assertThat(((ErrorResponseException) ex).errorResponse().code())
                    .isEqualTo("NoSuchKey"));
  }
}
