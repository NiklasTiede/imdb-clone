package app.popcornsociety.catalog.internal.search.projection;

import com.github.kagkarlsson.scheduler.exceptions.SerializationException;
import com.github.kagkarlsson.scheduler.serializer.JavaSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/** Reads durable tasks queued before the Java package rename as well as new tasks. */
final class MovieSearchProjectionSerializer implements Serializer {
  private final JavaSerializer writer = new JavaSerializer();

  @Override
  public byte[] serialize(Object data) {
    return writer.serialize(data);
  }

  @Override
  public <T> T deserialize(Class<T> clazz, byte[] serializedData) {
    if (serializedData == null) {
      return null;
    }
    try (var input =
        new ObjectInputStream(new ByteArrayInputStream(serializedData)) {
          @Override
          protected ObjectStreamClass readClassDescriptor()
              throws IOException, ClassNotFoundException {
            var descriptor = super.readClassDescriptor();
            return switch (descriptor.getName()) {
              case "com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionTaskData" ->
                  ObjectStreamClass.lookup(MovieSearchProjectionTaskData.class);
              case "com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionOperation" ->
                  ObjectStreamClass.lookup(MovieSearchProjectionOperation.class);
              default -> descriptor;
            };
          }
        }) {
      return clazz.cast(input.readObject());
    } catch (IOException | ClassNotFoundException | ClassCastException e) {
      throw new SerializationException("Failed to deserialize scheduled task", e);
    }
  }
}
