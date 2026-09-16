package app.popcornsociety.engagement.api;

import app.popcornsociety.shared.api.MessageResponse;
import app.popcornsociety.shared.api.PagedResponse;
import app.popcornsociety.shared.api.ResourceWriteResult;
import app.popcornsociety.shared.security.UserPrincipal;

public interface RatingService {

  ResourceWriteResult<RatingRecord> rateMovie(
      UserPrincipal currentAccount, Long movieId, RatingScore score);

  PagedResponse<RatingRecord> getRatingsByAccountId(Long accountId, int page, int size);

  MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId);
}
