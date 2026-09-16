package app.popcornsociety.engagement.api;

import app.popcornsociety.shared.api.MessageResponse;
import app.popcornsociety.shared.api.PagedResponse;
import app.popcornsociety.shared.api.ResourceWriteResult;
import app.popcornsociety.shared.security.UserPrincipal;

public interface WatchedMovieService {

  ResourceWriteResult<WatchedMovieRecord> watchMovie(Long movieId, UserPrincipal currentAccount);

  PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccountId(Long accountId, int page, int size);

  MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount);
}
