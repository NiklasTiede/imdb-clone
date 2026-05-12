package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;

public interface WatchedMovieService {

  WatchedMovieRecord watchMovie(Long movieId, UserPrincipal currentAccount);

  PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccountId(Long accountId, int page, int size);

  MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount);
}
