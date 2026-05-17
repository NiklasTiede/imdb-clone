package com.thecodinglab.imdbclone.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.account.api.AccountService;
import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.media.internal.MediaService;
import com.thecodinglab.imdbclone.media.internal.MediaStorageProperties;
import com.thecodinglab.imdbclone.media.internal.images.MovieImageConstants;
import com.thecodinglab.imdbclone.media.internal.images.ProfilePhotoConstants;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@SpringBootTest
class MediaServiceIntegrationTest extends BaseContainers {

  private static final Path MOVIE_IMAGE =
      Path.of("src/main/resources/api-calls/object-storage/raw-movie-image.jpg");
  private static final Path PROFILE_PHOTO =
      Path.of("src/main/resources/api-calls/object-storage/raw-profile-photo.jpeg");

  @Autowired private MediaService mediaService;

  @Autowired private AccountService accountService;

  @Autowired private MovieService movieService;

  @Autowired private MovieRepository movieRepository;

  @Autowired private AccountRepository accountRepository;

  @Autowired private S3Client s3Client;

  @Autowired private MediaStorageProperties storageProperties;

  @Test
  void storeMovieImage_updatesMovieTokenAndStoresExpectedObjects() throws Exception {
    var upload = imageUpload("image", "raw-movie-image.jpg", MOVIE_IMAGE);

    List<String> storedImages = mediaService.storeMovieImage(upload, 1L);

    var movie = movieRepository.getMovieById(1L);
    assertThat(movie.getImageUrlToken()).isNotBlank();
    assertThat(storedImages).hasSize(2);
    String detailImageName = MovieImageConstants.getDetailViewImageName(movie.getImageUrlToken());
    String thumbnailImageName = MovieImageConstants.getThumbNailImageName(movie.getImageUrlToken());
    assertThat(detailImageName).startsWith("movies/posters/");
    assertThat(detailImageName).endsWith("_size_600x900.jpg");
    assertThat(thumbnailImageName).startsWith("movies/posters/");
    assertThat(thumbnailImageName).endsWith("_size_120x180.jpg");
    assertObjectExists(detailImageName);
    assertObjectExists(thumbnailImageName);
  }

  @Test
  void storeMovieImage_replacesExistingImagesWithNewToken() throws Exception {
    mediaService.storeMovieImage(imageUpload("image", "raw-movie-image.jpg", MOVIE_IMAGE), 1L);
    String oldToken = movieRepository.getMovieById(1L).getImageUrlToken();
    String oldDetailImageName = MovieImageConstants.getDetailViewImageName(oldToken);
    String oldThumbnailImageName = MovieImageConstants.getThumbNailImageName(oldToken);

    mediaService.storeMovieImage(imageUpload("image", "raw-movie-image.jpg", MOVIE_IMAGE), 1L);

    String newToken = movieRepository.getMovieById(1L).getImageUrlToken();
    assertThat(newToken).isNotBlank().isNotEqualTo(oldToken);
    assertObjectDoesNotExist(oldDetailImageName);
    assertObjectDoesNotExist(oldThumbnailImageName);
    assertObjectExists(MovieImageConstants.getDetailViewImageName(newToken));
    assertObjectExists(MovieImageConstants.getThumbNailImageName(newToken));
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
  void deleteMovieImage_withoutExistingImageIsCleanNoOp() {
    var movie = movieRepository.getMovieById(2L);
    movie.setImageUrlToken(null);
    movieRepository.save(movie);

    String message = mediaService.deleteMovieImage(2L);

    assertThat(message).contains("No movie image");
    assertThat(movieRepository.getMovieById(2L).getImageUrlToken()).isNull();
  }

  @Test
  void storeProfilePhoto_updatesAccountTokenAndStoresExpectedObjects() throws Exception {
    var upload = imageUpload("image", "raw-profile-photo.jpeg", PROFILE_PHOTO);
    var currentUser = currentUser();

    List<String> storedImages = mediaService.storeProfilePhoto(upload, currentUser);

    var account = accountRepository.getAccountByUsername("test_user_two");
    assertThat(account.getImageUrlToken()).isNotBlank();
    assertThat(storedImages).hasSize(2);
    assertObjectExists(ProfilePhotoConstants.getDetailViewImageName(account.getImageUrlToken()));
    assertObjectExists(ProfilePhotoConstants.getThumbnailImageName(account.getImageUrlToken()));
  }

  @Test
  void storeProfilePhoto_replacesExistingImagesWithNewToken() throws Exception {
    UserPrincipal currentUser = currentUser();
    mediaService.storeProfilePhoto(
        imageUpload("image", "raw-profile-photo.jpeg", PROFILE_PHOTO), currentUser);
    String oldToken = accountRepository.getAccountByUsername("test_user_two").getImageUrlToken();
    String oldDetailImageName = ProfilePhotoConstants.getDetailViewImageName(oldToken);
    String oldThumbnailImageName = ProfilePhotoConstants.getThumbnailImageName(oldToken);

    mediaService.storeProfilePhoto(
        imageUpload("image", "raw-profile-photo.jpeg", PROFILE_PHOTO), currentUser);

    String newToken = accountRepository.getAccountByUsername("test_user_two").getImageUrlToken();
    assertThat(newToken).isNotBlank().isNotEqualTo(oldToken);
    assertObjectDoesNotExist(oldDetailImageName);
    assertObjectDoesNotExist(oldThumbnailImageName);
    assertObjectExists(ProfilePhotoConstants.getDetailViewImageName(newToken));
    assertObjectExists(ProfilePhotoConstants.getThumbnailImageName(newToken));
  }

  @Test
  void deleteProfilePhoto_clearsAccountTokenAndDeletesObjects() throws Exception {
    var upload = imageUpload("image", "raw-profile-photo.jpeg", PROFILE_PHOTO);
    var currentUser = currentUser();
    mediaService.storeProfilePhoto(upload, currentUser);

    String imageUrlToken =
        accountRepository.getAccountByUsername("test_user_two").getImageUrlToken();
    String detailImageName = ProfilePhotoConstants.getDetailViewImageName(imageUrlToken);
    String thumbnailImageName = ProfilePhotoConstants.getThumbnailImageName(imageUrlToken);

    mediaService.deleteProfilePhoto(currentUser);

    assertObjectDoesNotExist(detailImageName);
    assertObjectDoesNotExist(thumbnailImageName);
    assertThat(accountRepository.getAccountByUsername("test_user_two").getImageUrlToken()).isNull();
  }

  @Test
  void deleteProfilePhoto_withoutExistingImageIsCleanNoOp() {
    var account = accountRepository.getAccountByUsername("test_user_two");
    account.setImageUrlToken(null);
    accountRepository.save(account);

    String message = mediaService.deleteProfilePhoto(currentUser());

    assertThat(message).contains("No profile photo");
    assertThat(accountRepository.getAccountByUsername("test_user_two").getImageUrlToken()).isNull();
  }

  @Test
  void deleteAccount_deletesProfilePhotoObjects() throws Exception {
    Account account =
        accountRepository.save(
            new Account("media_delete_user", "media-delete@example.com", "password"));
    var currentUser = currentUser(account.getId(), account.getUsername(), account.getEmail());
    mediaService.storeProfilePhoto(
        imageUpload("image", "raw-profile-photo.jpeg", PROFILE_PHOTO), currentUser);

    String imageUrlToken =
        accountRepository.getAccountByUsername(account.getUsername()).getImageUrlToken();
    String detailImageName = ProfilePhotoConstants.getDetailViewImageName(imageUrlToken);
    String thumbnailImageName = ProfilePhotoConstants.getThumbnailImageName(imageUrlToken);

    accountService.deleteAccount(account.getUsername(), currentUser);

    assertObjectDoesNotExist(detailImageName);
    assertObjectDoesNotExist(thumbnailImageName);
  }

  @Test
  void deleteMovie_deletesMovieImageObjects() throws Exception {
    Movie movie =
        movieRepository.save(
            new Movie("Media delete movie", "Media delete movie", MovieType.MOVIE, 100));
    mediaService.storeMovieImage(
        imageUpload("image", "raw-movie-image.jpg", MOVIE_IMAGE), movie.getId());

    String imageUrlToken = movieRepository.getMovieById(movie.getId()).getImageUrlToken();
    String detailImageName = MovieImageConstants.getDetailViewImageName(imageUrlToken);
    String thumbnailImageName = MovieImageConstants.getThumbNailImageName(imageUrlToken);

    movieService.deleteMovie(movie.getId());

    assertObjectDoesNotExist(detailImageName);
    assertObjectDoesNotExist(thumbnailImageName);
  }

  private UserPrincipal currentUser() {
    return new UserPrincipal(
        2L,
        null,
        null,
        "test_user_two",
        "two@web.com",
        "password",
        false,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private UserPrincipal currentUser(Long accountId, String username, String email) {
    return new UserPrincipal(
        accountId,
        null,
        null,
        username,
        email,
        "password",
        false,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private MockMultipartFile imageUpload(String name, String fileName, Path image)
      throws IOException {
    return new MockMultipartFile(name, fileName, "image/jpeg", Files.newInputStream(image));
  }

  private void assertObjectExists(String objectName) {
    var stat =
        s3Client.headObject(
            HeadObjectRequest.builder()
                .bucket(storageProperties.bucketName())
                .key(objectName)
                .build());
    assertThat(stat.contentLength()).isPositive();
  }

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
}
