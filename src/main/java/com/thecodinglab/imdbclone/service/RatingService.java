package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.rating.RatingRecord;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface RatingService {

  RatingRecord rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score);

  Page<RatingRecord> getRatingsByAccount(String username, int page, int size);

  MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId);
}
