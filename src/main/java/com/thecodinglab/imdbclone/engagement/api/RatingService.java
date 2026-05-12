package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import java.math.BigDecimal;

public interface RatingService {

  RatingRecord rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score);

  PagedResponse<RatingRecord> getRatingsByAccountId(Long accountId, int page, int size);

  MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId);
}
