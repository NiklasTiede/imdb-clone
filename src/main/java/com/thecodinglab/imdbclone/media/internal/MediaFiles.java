package com.thecodinglab.imdbclone.media.internal;

import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.account.api.AccountImageToken;
import com.thecodinglab.imdbclone.account.api.events.AccountDeleted;
import com.thecodinglab.imdbclone.account.api.events.ProfileImageReplaced;
import com.thecodinglab.imdbclone.catalog.api.MovieImageService;
import com.thecodinglab.imdbclone.catalog.api.MovieImageToken;
import com.thecodinglab.imdbclone.catalog.api.events.MovieDeleted;
import com.thecodinglab.imdbclone.catalog.api.events.MovieImageReplaced;
import com.thecodinglab.imdbclone.media.internal.images.Image;
import com.thecodinglab.imdbclone.media.internal.images.ImageSize;
import com.thecodinglab.imdbclone.media.internal.images.MovieImageConstants;
import com.thecodinglab.imdbclone.media.internal.images.ProfilePhotoConstants;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaFiles implements MediaService {

  private final MediaObjects objects;
  private final MediaWork work;
  private final AccountImageService accountImageService;
  private final MovieImageService movieImageService;
  private final TransactionTemplate transactions;

  public MediaFiles(
      MediaObjects objects,
      MediaWork work,
      AccountImageService accountImageService,
      MovieImageService movieImageService,
      PlatformTransactionManager transactionManager) {
    this.objects = objects;
    this.work = work;
    this.accountImageService = accountImageService;
    this.movieImageService = movieImageService;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  @Override
  public List<String> storeProfilePhoto(MultipartFile file, UserPrincipal currentUser) {
    ImageSize.validateProfilePhoto(file);
    AccountImageToken accountImageToken = accountImageService.getProfileImageToken(currentUser);
    String imageUrlToken = Image.generateToken();

    List<Image> profilePhotos =
        Image.createImages(
            file,
            ProfilePhotoConstants.TARGET_SIZES,
            ProfilePhotoConstants.ASPECT_RATIO,
            ProfilePhotoConstants.BUCKET_DIRECTORY_NAME,
            imageUrlToken);

    work.registerUpload(MediaKind.PROFILE, imageUrlToken);
    return transactions.execute(
        status -> {
          work.lockUpload(MediaKind.PROFILE, imageUrlToken);
          List<String> storedImages = profilePhotos.stream().map(objects::store).toList();
          accountImageService.updateProfileImageToken(accountImageToken.accountId(), imageUrlToken);
          work.attached(MediaKind.PROFILE, imageUrlToken);
          return storedImages;
        });
  }

  @Override
  @Transactional
  public String deleteProfilePhoto(UserPrincipal currentUser) {

    AccountImageToken accountImageToken = accountImageService.getProfileImageToken(currentUser);
    if (accountImageToken.imageUrlToken() == null) {
      return "No profile photo of User with accountId [%d] exists"
          .formatted(accountImageToken.accountId());
    }

    accountImageService.clearProfileImageToken(accountImageToken.accountId());

    return "Profile Photos of User with accountId [%d] and imageUrlToken [%s] were deleted"
        .formatted(accountImageToken.accountId(), accountImageToken.imageUrlToken());
  }

  @Override
  public List<String> storeMovieImage(MultipartFile file, Long movieId) {

    movieImageService.getMovieImageToken(movieId);
    ImageSize.validateMovieImage(file);
    String posterImageToken = Image.generateToken();

    List<Image> movieImages =
        Image.createImages(
            file,
            MovieImageConstants.TARGET_SIZES,
            MovieImageConstants.ASPECT_RATIO,
            MovieImageConstants.BUCKET_DIRECTORY_NAME,
            posterImageToken);

    work.registerUpload(MediaKind.MOVIE, posterImageToken);
    return transactions.execute(
        status -> {
          work.lockUpload(MediaKind.MOVIE, posterImageToken);
          List<String> storedImages = movieImages.stream().map(objects::store).toList();
          movieImageService.updateMovieImageToken(movieId, posterImageToken);
          work.attached(MediaKind.MOVIE, posterImageToken);
          return storedImages;
        });
  }

  @Override
  @Transactional
  public String deleteMovieImage(Long movieId) {
    MovieImageToken movieImageToken = movieImageService.getMovieImageToken(movieId);
    if (movieImageToken.posterImageToken() == null) {
      return "No movie image of movie with movieId [%d] exists"
          .formatted(movieImageToken.movieId());
    }

    movieImageService.clearMovieImageToken(movieImageToken.movieId());

    return "Movie images of movie with movieId [%d] were deleted"
        .formatted(movieImageToken.movieId());
  }

  @EventListener
  public void on(AccountDeleted event) {
    work.retire(MediaKind.PROFILE, event.imageUrlToken());
  }

  @EventListener
  public void on(MovieDeleted event) {
    work.retire(MediaKind.MOVIE, event.posterImageToken());
  }

  @EventListener
  public void on(ProfileImageReplaced event) {
    work.imageChanged(MediaKind.PROFILE, event.previousToken(), event.currentToken());
  }

  @EventListener
  public void on(MovieImageReplaced event) {
    work.imageChanged(MediaKind.MOVIE, event.previousToken(), event.currentToken());
  }

  public String generateUrl(String imageName) {
    return objects.generateUrl(imageName);
  }
}
