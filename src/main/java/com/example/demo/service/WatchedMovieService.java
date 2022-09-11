package com.example.demo.service;

import com.example.demo.Payload.MessageResponse;
import com.example.demo.entity.WatchedMovie;
import com.example.demo.security.UserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface WatchedMovieService {

  WatchedMovie watchMovie(Long movieId, UserPrincipal currentAccount);

  List<WatchedMovie> getWatchedMoviesByAccount(UserPrincipal currentAccount);

  MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount);
}
