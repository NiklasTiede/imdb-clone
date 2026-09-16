package app.popcornsociety.media.internal;

import app.popcornsociety.media.internal.images.MovieImageConstants;
import app.popcornsociety.media.internal.images.ProfilePhotoConstants;
import java.util.List;

public enum MediaKind {
  MOVIE,
  PROFILE;

  public List<String> objectNames(String token) {
    return this == MOVIE
        ? List.of(
            MovieImageConstants.getDetailViewImageName(token),
            MovieImageConstants.getThumbNailImageName(token))
        : List.of(
            ProfilePhotoConstants.getDetailViewImageName(token),
            ProfilePhotoConstants.getThumbnailImageName(token));
  }
}
