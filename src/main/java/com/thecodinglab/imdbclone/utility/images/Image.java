package com.thecodinglab.imdbclone.utility.images;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

public class Image {

  private final String imageName;
  private final InputStream inputStream;
  private final int streamSize;
  private static final String contentType = "image/jpeg";
  private static final String imageType = "jpg";

  private Image(String imageName, InputStream inputStream, int streamSize) {
    this.imageName = imageName;
    this.inputStream = inputStream;
    this.streamSize = streamSize;
  }

  public static List<Image> createImages(
      MultipartFile file,
      List<Integer> scalingSizes,
      double aspectRatio,
      String targetDirectory,
      String imageUrlToken) {
    return scalingSizes.stream()
        .map(size -> createImage(file, size, aspectRatio, targetDirectory, imageUrlToken))
        .toList();
  }

  private static Image createImage(
      MultipartFile file,
      Integer scalingSize,
      double aspectRatio,
      String targetDirectory,
      String imageUrlToken) {

    // read image
    BufferedImage image = readImage(file);

    // normalize to aspectRatio
    BufferedImage normalizedImage = normalizeImage(image, aspectRatio);

    // rescale to provided size
    int targetWidth = scalingSize;
    int targetHeight = (int) (scalingSize / aspectRatio);
    ByteArrayOutputStream scaledImage =
        rescaleImage(normalizedImage, targetWidth, targetHeight, imageType);

    // set properties
    int streamSize = scaledImage.size();
    InputStream inputStream = new ByteArrayInputStream(scaledImage.toByteArray());
    String imageName =
        targetDirectory
            + imageUrlToken
            + "_size_"
            + targetWidth
            + "x"
            + targetHeight
            + "."
            + imageType; // TODO: refactor

    return new Image(imageName, inputStream, streamSize);
  }

  private static BufferedImage readImage(MultipartFile image) {
    BufferedImage bufferedImage;
    try {
      bufferedImage = ImageIO.read(image.getInputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bufferedImage;
  }

  private static BufferedImage normalizeImage(BufferedImage image, double aspectRatio) {

    double imageAspectRatio = (double) image.getWidth() / image.getHeight();

    int cropWidth =
        (int) (imageAspectRatio > aspectRatio ? image.getHeight() * aspectRatio : image.getWidth());
    int cropHeight =
        (int) (imageAspectRatio > aspectRatio ? image.getHeight() : image.getWidth() / aspectRatio);

    int cropX = (image.getWidth() - cropWidth) / 2;
    int cropY = (image.getHeight() - cropHeight) / 2;

    return image.getSubimage(cropX, cropY, cropWidth, cropHeight);
  }

  private static ByteArrayOutputStream rescaleImage(
      BufferedImage normalizedImage, int targetWidth, int targetHeight, String imageType) {

    BufferedImage resized =
        new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

    resized
        .createGraphics()
        .drawImage(
            normalizedImage.getScaledInstance(
                targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH),
            0,
            0,
            null);

    // Write images to output streams
    ByteArrayOutputStream originalStream = new ByteArrayOutputStream();
    try {
      ImageIO.write(resized, imageType, originalStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return originalStream;
  }

  public String getImageName() {
    return imageName;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public int getStreamSize() {
    return streamSize;
  }

  public String getContentType() {
    return contentType;
  }

  @Override
  public String toString() {
    return "Image{"
        + "imageName='"
        + imageName
        + '\''
        + ", inputStream="
        + inputStream
        + ", streamSize="
        + streamSize
        + '}';
  }
}
