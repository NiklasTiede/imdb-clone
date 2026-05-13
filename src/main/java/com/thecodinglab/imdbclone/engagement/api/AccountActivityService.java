package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public interface AccountActivityService {

  EngagementStats getStatsForAccount(Long accountId);

  PagedResponse<CommentRecord> getCommentsByAccountId(Long accountId, int page, int size);

  PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccountId(Long accountId, int page, int size);

  PagedResponse<RatingRecord> getRatingsByAccountId(Long accountId, int page, int size);
}
