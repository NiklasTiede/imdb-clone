package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

public class ImageSize {

  public static final int MIN_MOVIE_IMAGE_WIDTH = 600;
  public static final int MIN_MOVIE_IMAGE_HEIGHT = 900;

  //  public static final int MIN_PROFILE_PHOTO_WIDTH = 800;
  //  public static final int MIN_PROFILE_PHOTO_HEIGHT = 800;
  public static final int MIN_PROFILE_PHOTO_WIDTH = 500;
  public static final int MIN_PROFILE_PHOTO_HEIGHT = 500;

  public static void validateMovieImage(MultipartFile file) {
    BufferedImage image = readImage(file);

    if (image.getWidth() < MIN_MOVIE_IMAGE_WIDTH) {
      throw new BadRequestException(
          "Movie image cannot be less than [" + MIN_MOVIE_IMAGE_WIDTH + "] in width.");
    }
    if (image.getHeight() < MIN_MOVIE_IMAGE_HEIGHT) {
      throw new BadRequestException(
          "Movie image cannot be less than [" + MIN_MOVIE_IMAGE_HEIGHT + "] in height.");
    }
  }

  public static void validateProfilePhoto(MultipartFile file) {
    BufferedImage image = readImage(file);

    if (image.getWidth() < MIN_PROFILE_PHOTO_WIDTH) {
      throw new BadRequestException(
          "Profile photo cannot be less than [" + MIN_PROFILE_PHOTO_WIDTH + "] in width.");
    }
    if (image.getHeight() < MIN_PROFILE_PHOTO_HEIGHT) {
      throw new BadRequestException(
          "Profile photo cannot be less than [" + MIN_PROFILE_PHOTO_HEIGHT + "] in height.");
    }
  }

  // TODO: evtl. refactor into utils
  private static BufferedImage readImage(MultipartFile image) {
    BufferedImage bufferedImage;
    try {
      bufferedImage = ImageIO.read(new ByteArrayInputStream(image.getBytes()));
      return bufferedImage;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
