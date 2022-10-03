package com.example.demo.service;

import com.example.demo.entity.WatchedMovie;
import com.example.demo.payload.MessageResponse;
import com.example.demo.payload.PagedResponse;
import com.example.demo.payload.WatchedMovieRecord;
import com.example.demo.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public interface WatchedMovieService {

  WatchedMovie watchMovie(Long movieId, UserPrincipal currentAccount);

  PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccount(String username, int page, int size);

  MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount);
}
