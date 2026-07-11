package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;

public interface RatingService {

  RatingRecord rateMovie(UserPrincipal currentAccount, Long movieId, RatingScore score);

  PagedResponse<RatingRecord> getRatingsByAccountId(Long accountId, int page, int size);

  MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId);
}
