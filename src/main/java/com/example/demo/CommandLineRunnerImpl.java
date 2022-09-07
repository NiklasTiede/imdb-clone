package com.example.demo;

import com.example.demo.Payload.mapper.MovieMapper;
import com.example.demo.entity.*;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.*;
import com.example.demo.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CommandLineRunnerImpl implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineRunnerImpl.class);

  @Autowired private MovieSearchDao movieDao;

  @Autowired private MovieRepository movieRepository;

  @Autowired private AccountRepository accountRepository;

  @Autowired private RatingRepository ratingRepository;

  @Autowired private CommentRepository commentRepository;

  @Autowired private WatchedMovieRepository watchedMovieRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private VerificationTokenRepository verificationTokenRepository;

  @Autowired private RatingService ratingService;

  private final MovieMapper movieMapper;

  public CommandLineRunnerImpl(MovieMapper movieMapper) {
    this.movieMapper = movieMapper;
  }

  @Override
  public void run(String... arg0) {

    Account account =
        accountRepository
            .findByEmail("niklas@gmail.com")
            .orElseThrow(() -> new NotFoundException("bla"));

    //    List<Rating> ratingList = ratingRepository.findRatingsByAccount(account);
    //    System.out.println(ratingList);
    //    System.out.println(ratingList.get(0).getMovie().getId());
    //    System.out.println(ratingList.get(0).getRating());
    //    System.out.println(ratingList.get(1).getMovie().getId());
    //    System.out.println(ratingList.get(1).getRating());
    //
    //    List<Long> movieIds = new ArrayList<>();
    //    movieIds.add(1L);
    //    movieIds.add(2L);
    //
    //    List<Movie> movies = movieRepository.findMoviesByIdIn(movieIds);
    //    System.out.println(movies.get(0).getPrimaryTitle());
    //    System.out.println(movies.get(1).getPrimaryTitle());

    //    Rating rating = ratingRepository.findRatingByAccountIdAndMovieId(1L, 1457767L);
    //    System.out.println(rating.getRating());

    //    String bla = ratingService.rateMovie(1457767L, new BigDecimal("5.5"), 6L);
    //    System.out.println(bla);

    // generate base64-encoded secret:
    //    SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    //    String base64Key = Encoders.BASE64.encode(key.getEncoded());
    //    System.out.println(base64Key);
    //
    //    try {
    //      KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA512");
    //      Key key2 = keyGen.generateKey();
    //      System.out.println(key2);
    //      System.out.println(key2.getFormat());
    //      System.out.println(key2.getAlgorithm());
    //      System.out.println(Arrays.toString(key2.getEncoded()));
    //    } catch (NoSuchAlgorithmException e) {
    //      throw new RuntimeException(e);
    //    }
    //    String secret =
    //
    // "fKq+2kMG4sdb7yrZDOuBJxYJ6SquHrEMI5wE/N0x1fOzkXjxu8G0Aue6lLY+fjogSTyuF0sm1c6X0ccRwJPUnQ==";
    //    Key hmacKey =
    //        new SecretKeySpec(
    //            Base64.getDecoder().decode(secret), SignatureAlgorithm.HS512.getJcaName());
    //    System.out.println(hmacKey);
    //    System.out.println(Arrays.toString(hmacKey.getEncoded()));
    //
    //    String token = UUID.randomUUID().toString();
    //    System.out.println("Tokenlength: " + token.length());
    //    System.out.println("Token: " + token);

    //    VerificationToken verificationToken =
    // verificationTokenRepository.findById(1L).orElseThrow();
    //    System.out.println(verificationToken);
    //    System.out.println("enum: " + verificationToken.getVerificationType());
    //    System.out.println("enum: " + verificationToken.getVerificationType().name());
    //
    //    VerificationToken bla =
    //        verificationTokenRepository.findFirstByAccount_IdAndVerificationTypeOrderByIdDesc(
    //            1L, VerificationTypeEnum.EMAIL_CONFIRMATION);
    //    System.out.println(bla);
    //
    //    VerificationToken bla2 =
    //
    // verificationTokenRepository.findFirstByAccount_IdAndVerificationTypeOrderByExpiryDateDesc(
    //            1L, VerificationTypeEnum.EMAIL_CONFIRMATION);
    //    System.out.println(bla2);

    //    // Role
    //    roleRepository.save(new Role("ROLE_ADMIN"));
    //    roleRepository.save(new Role("ROLE_USER"));

    // movies
    //    Movie movie = movieRepository.findById(1457767L).get();
    //    System.out.println(movie);

    //    MovieRecord movieRecord = movieMapper.entityToDTO(movie);
    //    System.out.println(movieRecord);
    //    System.out.println(movieRecord.movieGenre());

    //    long startTime = System.currentTimeMillis();
    //    List<Movie> movies = movieDao.findByPrimaryTitleStartsWith("The Conjuring");
    //    System.out.println(movies.size());
    //    long stopTime = System.currentTimeMillis();
    //    long elapsedTime = stopTime - startTime;
    //    System.out.println("elapsedTime (ms): " + elapsedTime);

    //    Account account2 =
    //        accountRepository
    //            .findByEmail("niklastiede2@gmail.com")
    //            .orElseThrow(() -> new NotFoundException("bla"));
    //    System.out.println(account2.getEmail());
    //    System.out.println(account2.getId());
    //
    //    List<WatchedMovie> watchedMovies =
    // watchedMovieRepository.findAllByAccountId(account2.getId());
    //    System.out.println(watchedMovies.get(0).getId());
    //    System.out.println(watchedMovies.get(0).getCreatedAtInUtc());
    //    System.out.println(watchedMovies.get(0).getMovie().getId());
    //
    //    List<Long> longs =
    //        watchedMovies.stream().map(WatchedMovie::getId).map(WatchlistId::getMovieId).toList();
    //    System.out.println(longs);
    //
    //    List<WatchedMovie> watchedMovies1 =
    //        watchedMovieRepository.findAllByAccountIdOrderByCreatedAtInUtcDesc(account2.getId());
    //    System.out.println("kaboom");
    //    System.out.println(watchedMovies1.get(0).getCreatedAtInUtc());
    //    System.out.println(watchedMovies1.get(0).getMovie().getId());
    //    System.out.println(watchedMovies1.get(1).getCreatedAtInUtc());
    //    System.out.println(watchedMovies1.get(1).getMovie().getId());
    //
    //    System.out.println(watchedMovies1.get(1).getId().getMovieId());
    //    System.out.println(watchedMovies1.get(1).getId().getAccountId());
    //
    //    Movie movie2 = movieRepository.findById(1457767L).get();
    //    MovieRecord movieRecord2 = movieMapper.entityToDTO(movie2);
    //    System.out.println(movieRecord2);

    //    // accounts
    //    Account account = accountRepository.findById(1L).get();
    //    System.out.println(account.getFirstName());
    //    System.out.println(account.getModifiedAt());
    //
    //    Account userAcc = accountRepository.findByEmail("schnuggi@yahoo.de").orElseThrow();
    //    System.out.println(userAcc.getEmail());
    //
    //    String email = "schnuggi@yahoo.de";
    //    Account userAcc2 = accountRepository.findByUsernameOrEmail(email, email).orElseThrow();
    //    System.out.println(userAcc2.getEmail());
    //
    //    // ratings
    //    List<Rating> rating = ratingRepository.findAll();
    //    rating.stream().map(Rating::getRating).forEach(System.out::println);
    //
    //    List<Rating> ratingsByMovie = ratingRepository.findRatingsByMovie(movie);
    //    ratingsByMovie.stream().map(Rating::getRating).forEach(System.out::println);
    //
    //    List<Rating> ratingsByAccount = ratingRepository.findRatingsByAccount(account);
    //    ratingsByAccount.stream().map(Rating::getRating).forEach(System.out::println);
    //
    //    // comments
    //    List<Comment> allComments = commentRepository.findAll();
    //    allComments.stream().map(Comment::getMessage).forEach(System.out::println);
    //
    //    List<Comment> movieComments = commentRepository.findCommentsByMovie(movie);
    //    movieComments.stream().map(Comment::getMessage).forEach(System.out::println);
    //
    //    List<Comment> accountComments = commentRepository.findCommentsByAccount(account);
    //    accountComments.stream().map(Comment::getMessage).forEach(System.out::println);
    //
    //    // watchlist
    //    List<Watchlist> watchlists = watchlistRepository.findAll();
    //    watchlists.stream().map(Watchlist::getCreatedAt).forEach(System.out::println);
    //
    //    System.out.println("OneToMany of composite PKs...");
    //    List<Watchlist> watchlists2 = watchlistRepository.findAllByMovieId(1457767L);
    //    watchlists2.stream()
    //        .map(Watchlist::getAccount)
    //        .map(Account::getEmail)
    //        .forEach(System.out::println);
    //
    //    List<Watchlist> watchlists3 = watchlistRepository.findAllByAccountId(1L);
    //    watchlists3.stream()
    //        .map(Watchlist::getMovie)
    //        .map(Movie::getPrimaryTitle)
    //        .forEach(System.out::println);

    // test timezone stuff:
    //    System.out.println("timezone stuff");
    //    System.out.println("new Date(): " + new Date()); // works with UTC
    //    Instant instant = new Date().toInstant();
    //    System.out.println("instant: " + instant);
    //
    //    ZoneId zoneId = ZoneId.of("Europe/Paris");
    //    LocalDateTime bla = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    //    System.out.println("bla:" + bla);
    //
    //    Instant convertedInstant = instant.atZone(zoneId).toInstant();
    //    System.out.println("instant after timezone: " + convertedInstant);
    //
    //    System.out.println("LocalDateTime.now(): " + LocalDateTime.now());
    //    LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(30);
    //    System.out.println("LocalDateTime.now().plusMinutes(30): " + localDateTime);
    //
    //    // java.time.instant: time in UTC
    //    Instant instant3 = Instant.now();
    //    System.out.println("instant3: " + instant3);
    //    instant3.plus(30, ChronoUnit.MINUTES);
    //    System.out.println(instant3);
    //
    //    Instant aTime = Instant.now().plus(30, ChronoUnit.MINUTES);

    //    // how are the times set as default in create table differing?
    //    String emailOrUsername2 = "niklastiede2@gmail.com";
    //    Account account2 =
    //        accountRepository.findByUsernameOrEmail(emailOrUsername2,
    // emailOrUsername2).orElseThrow();
    //    System.out.println(account2);

    //    Instant createdAt = account2.getCreatedAtInUtc();
    //    System.out.println("createdAt: " + createdAt);
    //    System.out.println("createdAt: " + createdAt);
  }
}
