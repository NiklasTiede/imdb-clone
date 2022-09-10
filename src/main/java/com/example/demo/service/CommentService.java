package com.example.demo.service;

import com.example.demo.Payload.CreateCommentRequest;
import com.example.demo.Payload.UpdateCommentRequest;
import com.example.demo.entity.Comment;
import com.example.demo.security.UserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface CommentService {

  Comment getComment(Long commentId);

  List<Comment> getCommentsByMovieId(Long movieId);

  List<Comment> getCommentsByAccount(UserPrincipal currentAccount);

  Comment createComment(Long movieId, CreateCommentRequest request, UserPrincipal currentAccount);

  Comment updateComment(Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount);

  String deleteComment(Long commentId, UserPrincipal currentAccount);
}
