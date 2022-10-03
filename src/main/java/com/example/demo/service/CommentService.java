package com.example.demo.service;

import com.example.demo.payload.*;
import com.example.demo.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public interface CommentService {

  CommentRecord getComment(Long commentId);

  PagedResponse<CommentRecord> getCommentsByMovieId(Long movieId, int page, int size);

  CommentRecord createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount);

  PagedResponse<CommentRecord> getCommentsByAccount(String username, int page, int size);

  CommentRecord updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount);

  MessageResponse deleteComment(Long commentId, UserPrincipal currentAccount);
}
