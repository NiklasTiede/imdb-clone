package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.ACCOUNT_ID;
import static com.thecodinglab.imdbclone.utility.Log.RATING_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.entity.Rating;
import com.thecodinglab.imdbclone.exception.domain.BadRequestException;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import com.thecodinglab.imdbclone.exception.domain.UnauthorizedException;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.mapper.CustomRatingMapper;
import com.thecodinglab.imdbclone.payload.rating.RatingRecord;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.repository.RatingRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.RatingService;
import com.thecodinglab.imdbclone.validation.Pagination;
import java.math.BigDecimal;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class RatingServiceImpl implements RatingService {

  private static final Logger logger = LoggerFactory.getLogger(RatingServiceImpl.class);

  private final MovieRepository movieRepository;
  private final AccountRepository accountRepository;
  private final RatingRepository ratingRepository;
  private final CustomRatingMapper ratingMapper;

  public RatingServiceImpl(
      MovieRepository movieRepository,
      AccountRepository accountRepository,
      RatingRepository ratingRepository,
      CustomRatingMapper ratingMapper) {
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
    this.ratingRepository = ratingRepository;
    this.ratingMapper = ratingMapper;
  }

  @Override
  public Rating rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score) {
    if (score.floatValue() < 0 || score.floatValue() > 10.1) {
      throw new BadRequestException("Score must be between 0 and 10");
    } else {
      Movie movie = movieRepository.getMovieById(movieId);
      Account account = accountRepository.getAccount(currentAccount);
      Rating rating = Rating.create(score, movie, account);
      Rating savedRating = ratingRepository.save(rating);
      logger.info("rating with [{}] was created.", kv(RATING_ID, savedRating.getId()));
      return savedRating;
    }
  }

  @Override
  public PagedResponse<RatingRecord> getRatingsByAccount(String username, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Account account = accountRepository.getAccountByUsername(username);
    Page<Rating> ratings = ratingRepository.findRatingsByAccount(account, pageable);
    logger.info(
        "[{}] ratings from account with [{}] were retrieved.",
        ratings.getContent().size(),
        kv(ACCOUNT_ID, account.getId()));
    Page<RatingRecord> ratingRecordPage = ratings.map(ratingMapper::entityToDTO);
    return new PagedResponse<>(
        ratingRecordPage.getContent(),
        ratingRecordPage.getNumber(),
        ratingRecordPage.getSize(),
        ratingRecordPage.getTotalElements(),
        ratingRecordPage.getTotalPages(),
        ratingRecordPage.isLast());
  }

  @Override
  public MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId) {
    Rating rating =
        ratingRepository
            .findRatingByAccountIdAndMovieId(currentAccount.getId(), movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Rating with movieId ["
                            + movieId
                            + "] and accountId ["
                            + currentAccount.getId()
                            + "] not found in database."));
    if (Objects.equals(rating.getAccount().getId(), currentAccount.getId())
        || Boolean.TRUE.equals(UserPrincipal.isCurrentAccountAdmin(currentAccount))) {
      ratingRepository.delete(rating);
      logger.info("rating with [{}] was deleted.", kv(RATING_ID, rating.getId()));
      return new MessageResponse(
          "WatchedMovie with movieId ["
              + movieId
              + "] and accountId ["
              + currentAccount.getId()
              + "] was deleted");
    } else {
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to delete this resource.");
    }
  }
}
