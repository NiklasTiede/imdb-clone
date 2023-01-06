package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.Rating;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.rating.RatingRecord;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public interface RatingService {

  Rating rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score);

  PagedResponse<RatingRecord> getRatingsByAccount(String username, int page, int size);

  MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId);
}
