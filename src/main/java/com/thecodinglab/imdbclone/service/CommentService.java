package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.comment.CommentRecord;
import com.thecodinglab.imdbclone.payload.comment.CreateCommentRequest;
import com.thecodinglab.imdbclone.payload.comment.UpdateCommentRequest;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface CommentService {

  CommentRecord getComment(Long commentId);

  Page<CommentRecord> getCommentsByMovieId(Long movieId, int page, int size);

  CommentRecord createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount);

  Page<CommentRecord> getCommentsByAccount(String username, int page, int size);

  CommentRecord updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount);

  MessageResponse deleteComment(Long commentId, UserPrincipal currentAccount);
}
