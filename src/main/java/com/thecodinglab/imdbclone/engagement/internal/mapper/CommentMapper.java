package com.thecodinglab.imdbclone.engagement.internal.mapper;

import com.thecodinglab.imdbclone.engagement.api.CommentRecord;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Comment;
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
