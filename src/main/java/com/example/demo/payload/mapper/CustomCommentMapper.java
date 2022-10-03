package com.example.demo.payload.mapper;

import com.example.demo.entity.Comment;
import com.example.demo.payload.CommentRecord;
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
