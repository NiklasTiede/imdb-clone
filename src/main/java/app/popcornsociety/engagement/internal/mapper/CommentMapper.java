package app.popcornsociety.engagement.internal.mapper;

import app.popcornsociety.engagement.api.CommentRecord;
import app.popcornsociety.engagement.internal.persistence.Comment;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

  public CommentRecord entityToDTO(Comment comment) {
    return new CommentRecord(
        comment.getId(),
        comment.getMessage(),
        comment.getAccountId(),
        comment.getMovieId(),
        comment.getCreatedAtInUtc(),
        comment.getModifiedAtInUtc());
  }

  public List<CommentRecord> entityToDTO(List<Comment> comments) {
    return comments.stream().map(this::entityToDTO).toList();
  }
}
