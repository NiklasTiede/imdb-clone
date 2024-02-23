package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.WatchedMovie;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.watchlist.WatchedMovieRecord;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface WatchedMovieService {

  WatchedMovie watchMovie(Long movieId, UserPrincipal currentAccount);

  Page<WatchedMovieRecord> getWatchedMoviesByAccount(String username, int page, int size);

  MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount);
}
