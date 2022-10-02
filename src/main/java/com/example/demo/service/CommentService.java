package com.example.demo.service;

import com.example.demo.Payload.*;
import com.example.demo.security.UserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface CommentService {

  CommentRecord getComment(Long commentId);

  List<CommentRecord> getCommentsByMovieId(Long movieId);

  CommentRecord createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount);

  PagedResponse<CommentRecord> getCommentsByAccount(String username, int page, int size);

  CommentRecord updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount);

  MessageResponse deleteComment(Long commentId, UserPrincipal currentAccount);
}
