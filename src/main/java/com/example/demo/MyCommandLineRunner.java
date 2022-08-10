package com.example.demo;

import com.example.demo.dto.MovieRecord;
import com.example.demo.dto.mapper.MovieMapper;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MyCommandLineRunner implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MyCommandLineRunner.class);

  @Autowired private MovieSearchDao movieDao;

  @Autowired private MovieRepository movieRepository;

  @Autowired private AccountRepository accountRepository;

  @Autowired private RatingRepository ratingRepository;

  @Autowired private CommentRepository commentRepository;

  @Autowired private WatchlistRepository watchlistRepository;

  private final MovieMapper movieMapper;

  public MyCommandLineRunner(MovieMapper movieMapper) {
    this.movieMapper = movieMapper;
  }

  @Override
  public void run(String... arg0) {

    // movies
    Movie movie = movieRepository.findById(1457767L).get();
    System.out.println(movie);

    MovieRecord movieRecord = movieMapper.entityToDTO(movie);
    System.out.println(movieRecord);

    long startTime = System.currentTimeMillis();
    List<Movie> movies = movieDao.findByPrimaryTitleStartsWith("The Conjuring");
    System.out.println(movies.size());
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    System.out.println("elapsedTime (ms): " + elapsedTime);

    // accounts
    Account account = accountRepository.findById(1L).get();
    System.out.println(account.getFirstName());
    System.out.println(account.getModifiedAt());

    Account userAcc = accountRepository.findByEmail("schnuggi@yahoo.de");
    System.out.println(userAcc.getEmail());

    // ratings
    List<Rating> rating = ratingRepository.findAll();
    rating.stream().map(Rating::getRating).forEach(System.out::println);

    List<Rating> ratingsByMovie = ratingRepository.findRatingsByMovie(movie);
    ratingsByMovie.stream().map(Rating::getRating).forEach(System.out::println);

    List<Rating> ratingsByAccount = ratingRepository.findRatingsByAccount(account);
    ratingsByAccount.stream().map(Rating::getRating).forEach(System.out::println);

    // comments
    List<Comment> allComments = commentRepository.findAll();
    allComments.stream().map(Comment::getMessage).forEach(System.out::println);

    List<Comment> movieComments = commentRepository.findCommentsByMovie(movie);
    movieComments.stream().map(Comment::getMessage).forEach(System.out::println);

    List<Comment> accountComments = commentRepository.findCommentsByAccount(account);
    accountComments.stream().map(Comment::getMessage).forEach(System.out::println);

    // watchlist
    List<Watchlist> watchlists = watchlistRepository.findAll();
    watchlists.stream().map(Watchlist::getCreatedAt).forEach(System.out::println);

    System.out.println("OneToMany of composite PKs...");
    List<Watchlist> watchlists2 = watchlistRepository.findAllByMovieId(1457767L);
    watchlists2.stream()
        .map(Watchlist::getAccount)
        .map(Account::getEmail)
        .forEach(System.out::println);

    List<Watchlist> watchlists3 = watchlistRepository.findAllByAccountId(1L);
    watchlists3.stream()
        .map(Watchlist::getMovie)
        .map(Movie::getPrimaryTitle)
        .forEach(System.out::println);
  }
}
