package com.example.demo.service;

import com.example.demo.entity.Rating;
import com.example.demo.payload.MessageResponse;
import com.example.demo.payload.PagedResponse;
import com.example.demo.payload.RatingRecord;
import com.example.demo.security.UserPrincipal;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public interface RatingService {

  Rating rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score);

  PagedResponse<RatingRecord> getRatingsByAccount(String username, int page, int size);

  MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId);
}
