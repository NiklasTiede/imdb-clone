package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.identity.api.UserPrincipal;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.watchlist.WatchedMovieRecord;

public interface WatchedMovieService {

  WatchedMovieRecord watchMovie(Long movieId, UserPrincipal currentAccount);

  PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccount(String username, int page, int size);

  MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount);
}
