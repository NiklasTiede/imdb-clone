package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;

public interface CommentService {

  CommentRecord getComment(Long commentId);

  PagedResponse<CommentRecord> getCommentsByMovieId(Long movieId, int page, int size);

  CommentRecord createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount);

  PagedResponse<CommentRecord> getCommentsByAccountId(Long accountId, int page, int size);

  CommentRecord updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount);

  MessageResponse deleteComment(Long commentId, UserPrincipal currentAccount);
}
