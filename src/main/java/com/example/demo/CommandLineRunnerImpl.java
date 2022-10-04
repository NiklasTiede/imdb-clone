package com.example.demo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.demo.elasticsearch.EsClientConfig;
import com.example.demo.elasticsearch.EsUtils;
import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.payload.CommentRecord;
import com.example.demo.payload.mapper.CustomCommentMapper;
import com.example.demo.payload.mapper.MovieMapper;
import com.example.demo.repository.*;
import com.example.demo.service.CommentService;
import com.example.demo.service.RatingService;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class CommandLineRunnerImpl implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineRunnerImpl.class);

  private final MovieSearchDao movieDao;
  private final MovieRepository movieRepository;
  private final AccountRepository accountRepository;
  private final RatingRepository ratingRepository;
  private final CommentRepository commentRepository;
  private final WatchedMovieRepository watchedMovieRepository;
  private final RoleRepository roleRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final RatingService ratingService;
  private final MovieMapper movieMapper;
  private final MovieSearchDao movieSearchDao;
  private final CommentService commentService;
  private final EntityManager entityManager;

  private final CustomCommentMapper commentMapper;

  private final EsUtils esUtils;
  private final ElasticsearchClient esClient;

  public CommandLineRunnerImpl(
      MovieSearchDao movieDao,
      MovieRepository movieRepository,
      AccountRepository accountRepository,
      RatingRepository ratingRepository,
      CommentRepository commentRepository,
      WatchedMovieRepository watchedMovieRepository,
      RoleRepository roleRepository,
      VerificationTokenRepository verificationTokenRepository,
      RatingService ratingService,
      MovieMapper movieMapper,
      MovieSearchDao movieSearchDao,
      CommentService commentService,
      EntityManager entityManager,
      CustomCommentMapper commentMapper,
      EsUtils esUtils,
      EsClientConfig esClientConfig) {
    this.movieDao = movieDao;
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
    this.ratingRepository = ratingRepository;
    this.commentRepository = commentRepository;
    this.watchedMovieRepository = watchedMovieRepository;
    this.roleRepository = roleRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.ratingService = ratingService;
    this.movieMapper = movieMapper;
    this.movieSearchDao = movieSearchDao;
    this.commentService = commentService;
    this.entityManager = entityManager;
    this.commentMapper = commentMapper;
    this.esUtils = esUtils;
    this.esClient = esClientConfig.getClient();
  }

  @Override
  public void run(String... arg0) {

    Account account = accountRepository.findById(6L).orElseThrow();

    Comment comment = commentRepository.getCommentById(2L);
    System.out.println(comment.getId());
    System.out.println(comment.getMessage());
    System.out.println(comment.getAccount().getId());
    System.out.println(comment.getMovie().getId());

    CommentRecord commentRecord = commentMapper.entityToDTO(comment);

    //    CommentRecord commentRecord = commentMapper.entityToDTO(comment);
    System.out.println(commentRecord);
    System.out.println(commentRecord.id());
    System.out.println(commentRecord.message());
    System.out.println(commentRecord.accountId());
    System.out.println(commentRecord.movieId());

    Pageable pageable = PageRequest.of(0, 30, Sort.Direction.DESC, "createdAtInUtc");
    Page<Comment> comments =
        commentRepository.findCommentsByAccountOrderByCreatedAtInUtc(account, pageable);

    System.out.println(comments);
    System.out.println(comments.getContent());
    System.out.println(comments.getNumber());
    System.out.println(comments.getSize());
    System.out.println(comments.isLast());
    System.out.println(comments.getTotalElements());
    System.out.println(comments.getTotalPages());

    Long bla = commentRepository.countCommentsByAccount(account);
    System.out.println("count: " + bla);

    //    // bulk index
    //    List<Long> movieIds = new ArrayList<>();
    //    movieIds.add(3235888L);
    //    movieIds.add(2494362L);
    //    movieIds.add(1396484L);
    //    movieIds.add(1457767L);
    //    movieIds.add(2872718L);
    //    List<Movie> movies = movieRepository.findAllById(movieIds);
    //    esUtils.indexMovies(movies);
    //
    //    // index single document
    //    Movie movie = movieRepository.getMovieById(2872718L);
    //    esUtils.indexMovie(movie);
    //
    //    // search by id:
    //    Movie bla = esUtils.getMovieDocumentById(2872718L);
    //    System.out.println(bla.getPrimaryTitle());
    //
    //    // search by text
    //    String searchText = "it";
    //    List<Movie> movies1 = esUtils.searchMoviesByPrimaryTitle(searchText);
    //    System.out.println(movies1.size());
    //
    //    // range query
    //    float minRating = 5.0F;
    //    float maxRating = 8.0F;
    //    List<Movie> moviesBetweenRatings = esUtils.searchMoviesByRatingRange(minRating,
    // maxRating);
    //    System.out.println(moviesBetweenRatings.size());
    //
    //    System.out.println("availableProcessors: " + Runtime.getRuntime().availableProcessors());

    //        // making a searchRequest
    //        String searchText2 = "bike";
    //        double maxPrice = 200.0;
    //
    //        // Search by product name
    //        Query byName = MatchQuery.of(m -> m.field("name").query(searchText2))._toQuery();
    //
    //        // Search by max price
    //        Query byMaxPrice = RangeQuery.of(r ->
    //     r.field("price").gte(JsonData.of(maxPrice)))._toQuery();
    //
    //        // Combine name and price queries to search the product index
    //        SearchResponse<Todo> response =
    //                null;
    //        try {
    //          response = esClient.search(
    //                  s -> s.index("products").query(q -> q.bool(b ->
    //     b.must(byName).must(byMaxPrice))),
    //                  Todo.class);
    //        } catch (IOException e) {
    //          throw new RuntimeException(e);
    //        }
    //
    //        List<Hit<Todo>> hits = response.hits().hits();
    //        for (Hit<Todo> hit : hits) {
    //          Todo todo = hit.source();
    //          LOGGER.info(
    //                  "Found todo " + (todo != null ? todo.getTitle() : null) + ", score " +
    //     hit.score());
    //        }
    //
    //        // aggregations
    //        String searchText3 = "bike";
    //
    //        Query query = MatchQuery.of(m -> m.field("name").query(searchText3))._toQuery();
    //
    //        try {
    //          SearchResponse<Void> response2 =
    //                  esClient.search(
    //                          b ->
    //                                  b.index("products")
    //                                          .size(0)
    //                                          .query(query)
    //                                          .aggregations(
    //                                                  "price-histogram", a -> a.histogram(h ->
    //     h.field("price").interval(50.0))),
    //                          Void.class);
    //        } catch (IOException e) {
    //          throw new RuntimeException(e);
    //        }
    //
    //        List<HistogramBucket> buckets =
    //                response.aggregations().get("price-histogram").histogram().buckets().array();
    //
    //        for (HistogramBucket bucket : buckets) {
    //          LOGGER.info("There are " + bucket.docCount() + " bikes under " + bucket.key());
    //        }

  }
}
