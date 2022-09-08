package com.example.demo.service;

import com.example.demo.entity.Rating;
import com.example.demo.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface RatingService {

  Rating rateMovie(UserPrincipal currentUser, Long movieId, BigDecimal score);

  List<Rating> getRatingsByAccount(UserPrincipal currentUser);

  void deleteRating(UserPrincipal currentUser, Long movieId);
}
