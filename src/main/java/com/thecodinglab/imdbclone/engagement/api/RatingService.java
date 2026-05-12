package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.math.BigDecimal;

public interface RatingService {

  RatingRecord rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score);

  PagedResponse<RatingRecord> getRatingsByAccountId(Long accountId, int page, int size);

  MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId);
}
