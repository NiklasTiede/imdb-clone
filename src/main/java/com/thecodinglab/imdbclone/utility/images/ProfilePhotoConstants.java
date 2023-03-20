package com.thecodinglab.imdbclone.utility.images;

import java.util.Arrays;
import java.util.List;

public class ProfilePhotoConstants {
  public static final int DETAIL_VIEW_WIDTH = 800;
  public static final int DETAIL_VIEW_HEIGHT = 800;

  public static final int THUMBNAIL_WIDTH = 120;
  public static final int THUMBNAIL_HEIGHT = 120;

  public static final List<Integer> TARGET_SIZES =
      Arrays.asList(DETAIL_VIEW_WIDTH, THUMBNAIL_WIDTH);

  public static final double ASPECT_RATIO = 1.0;

  public static final String FORMAT = "jpg";

  public static final String BUCKET_DIRECTORY_NAME = "profile-photos/";

  public static String IMAGE_NAME_DETAIL_VIEW(String imageUrlToken) {
    return BUCKET_DIRECTORY_NAME
        + imageUrlToken
        + "_size_"
        + DETAIL_VIEW_WIDTH
        + "x"
        + DETAIL_VIEW_HEIGHT
        + "."
        + FORMAT;
  }

  public static String IMAGE_NAME_THUMBNAIL(String imageUrlToken) {
    return BUCKET_DIRECTORY_NAME
        + imageUrlToken
        + "_size_"
        + THUMBNAIL_WIDTH
        + "x"
        + THUMBNAIL_HEIGHT
        + "."
        + FORMAT;
  }
}
