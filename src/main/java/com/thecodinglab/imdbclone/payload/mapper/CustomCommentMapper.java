package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.entity.Comment;
import com.thecodinglab.imdbclone.payload.comment.CommentRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomCommentMapper {

  public CommentRecord entityToDTO(Comment comment) {
    return new CommentRecord(
        comment.getId(),
        comment.getMessage(),
        comment.getAccount().getId(),
        comment.getMovie().getId());
  }

  public List<CommentRecord> entityToDTO(List<Comment> comments) {
    return comments.stream().map(this::entityToDTO).toList();
  }
}
