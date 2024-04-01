package com.thecodinglab.imdbclone.utility.images;

import java.text.MessageFormat;
import java.util.List;

public class ProfilePhotoConstants {
  public static final int DETAIL_VIEW_WIDTH = 800;
  public static final int DETAIL_VIEW_HEIGHT = 800;

  public static final int THUMBNAIL_WIDTH = 120;
  public static final int THUMBNAIL_HEIGHT = 120;

  public static final List<Integer> TARGET_SIZES = List.of(DETAIL_VIEW_WIDTH, THUMBNAIL_WIDTH);

  public static final double ASPECT_RATIO = 1.0;

  public static final String FORMAT = "jpg";

  public static final String BUCKET_DIRECTORY_NAME = "profile-photos/";

  private ProfilePhotoConstants() {}

  public static String getDetailViewImageName(String imageUrlToken) {
    return MessageFormat.format(
        "{0}{1}_size_{2}x{3}.{4}",
        BUCKET_DIRECTORY_NAME, imageUrlToken, DETAIL_VIEW_WIDTH, DETAIL_VIEW_HEIGHT, FORMAT);
  }

  public static String getThumbnailImageName(String imageUrlToken) {
    return MessageFormat.format(
        "{0}{1}_size_{2}x{3}.{4}",
        BUCKET_DIRECTORY_NAME, imageUrlToken, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, FORMAT);
  }
}
