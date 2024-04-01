package com.thecodinglab.imdbclone.utility.images;

import com.thecodinglab.imdbclone.exception.domain.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

public class Image {

  private final String imageName;
  private final InputStream inputStream;
  private final int streamSize;
  private static final String CONTENT_TYPE = "image/jpeg";
  private static final String IMAGE_TYPE = "jpg";

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
    ByteArrayOutputStream scaledImage = rescaleImage(normalizedImage, targetWidth, targetHeight);

    // set properties
    int streamSize = scaledImage.size();
    InputStream inputStream = new ByteArrayInputStream(scaledImage.toByteArray());
    String imageName =
        MessageFormat.format(
            "{0}{1}_size_{2}x{3}.{4}",
            targetDirectory, imageUrlToken, targetWidth, targetHeight, IMAGE_TYPE);

    return new Image(imageName, inputStream, streamSize);
  }

  public static BufferedImage readImage(MultipartFile image) {
    try {
      return ImageIO.read(image.getInputStream());
    } catch (IOException ex) {
      throw new ImageProcessingException("Failed to read image: " + ex.getMessage());
    }
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
      BufferedImage normalizedImage, int targetWidth, int targetHeight) {

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
      ImageIO.write(resized, Image.IMAGE_TYPE, originalStream);
    } catch (IOException ex) {
      throw new ImageProcessingException(
          "Failed to write bytestream while rescaling image: " + ex.getMessage());
    }
    return originalStream;
  }

  public static String generateToken() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[25];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return token.replace("-", "y").replace("_", "z").substring(0, 30);
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
    return CONTENT_TYPE;
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
