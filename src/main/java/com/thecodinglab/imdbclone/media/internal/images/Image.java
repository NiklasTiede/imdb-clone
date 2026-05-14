package com.thecodinglab.imdbclone.media.internal.images;

import com.thecodinglab.imdbclone.shared.error.ImageProcessingException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
    BufferedImage image = readImage(file);
    return scalingSizes.stream()
        .map(size -> createImage(image, size, aspectRatio, targetDirectory, imageUrlToken))
        .toList();
  }

  private static Image createImage(
      BufferedImage image,
      Integer scalingSize,
      double aspectRatio,
      String targetDirectory,
      String imageUrlToken) {

    BufferedImage normalizedImage = normalizeImage(image, aspectRatio);
    int targetWidth = scalingSize;
    int targetHeight = (int) (scalingSize / aspectRatio);
    ByteArrayOutputStream scaledImage = rescaleImage(normalizedImage, targetWidth, targetHeight);

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
      BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
      if (bufferedImage == null) {
        throw new ImageProcessingException("Failed to read image: unsupported image content");
      }
      return bufferedImage;
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

    Graphics2D graphics = resized.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.drawImage(normalizedImage, 0, 0, targetWidth, targetHeight, null);
    } finally {
      graphics.dispose();
    }

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
