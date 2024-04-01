package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.exception.domain.BadRequestException;
import com.thecodinglab.imdbclone.utility.images.Image;
import java.awt.image.BufferedImage;
import org.springframework.web.multipart.MultipartFile;

public class ImageSize {

  public static final int MIN_MOVIE_IMAGE_WIDTH = 600;
  public static final int MIN_MOVIE_IMAGE_HEIGHT = 900;

  public static final int MIN_PROFILE_PHOTO_WIDTH = 500;
  public static final int MIN_PROFILE_PHOTO_HEIGHT = 500;

  private ImageSize() {}

  public static void validateMovieImage(MultipartFile file) {
    BufferedImage image = Image.readImage(file);

    if (image.getWidth() < MIN_MOVIE_IMAGE_WIDTH) {
      throw new BadRequestException(
          "Movie image cannot be less than [%d] in width.".formatted(MIN_MOVIE_IMAGE_WIDTH));
    }
    if (image.getHeight() < MIN_MOVIE_IMAGE_HEIGHT) {
      throw new BadRequestException(
          "Movie image cannot be less than [%d] in height.".formatted(MIN_MOVIE_IMAGE_HEIGHT));
    }
  }

  public static void validateProfilePhoto(MultipartFile file) {
    BufferedImage image = Image.readImage(file);

    if (image.getWidth() < MIN_PROFILE_PHOTO_WIDTH) {
      throw new BadRequestException(
          "Profile photo cannot be less than [%d] in width.".formatted(MIN_PROFILE_PHOTO_WIDTH));
    }
    if (image.getHeight() < MIN_PROFILE_PHOTO_HEIGHT) {
      throw new BadRequestException(
          "Profile photo cannot be less than [%d] in height.".formatted(MIN_PROFILE_PHOTO_HEIGHT));
    }
  }
}
