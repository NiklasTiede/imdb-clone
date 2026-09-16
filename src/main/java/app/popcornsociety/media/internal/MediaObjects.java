package app.popcornsociety.media.internal;

import app.popcornsociety.media.internal.images.Image;

/** External object storage Seam; deletion of an already absent object succeeds. */
public interface MediaObjects {
  String store(Image image);

  void delete(MediaKind kind, String token);

  String generateUrl(String imageName);
}
