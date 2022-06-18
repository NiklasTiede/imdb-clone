// package com.example.demo;
//
// import com.example.demo.model.Movie;
// import com.example.demo.model.Rating;
// import com.example.demo.model.User;
// import com.example.demo.repository.MovieRepository;
// import com.example.demo.repository.UserRepository;
// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;
//
// @Component
// public class MovieRepositoryCommandLineRunner implements CommandLineRunner {
//
//  private static final Logger LOGGER =
//      LoggerFactory.getLogger(MovieRepositoryCommandLineRunner.class);
//
//  @Autowired private MovieRepository movieRepository;
//
//  @Autowired private UserRepository userRepository;
//
//  @Override
//  public void run(String... arg0) {
//
//    Movie movie1 = new Movie("Indiana Jones and the Last Crusade", "1989");
//    Movie insert1 = movieRepository.save(movie1);
//    LOGGER.info("New Movie is created: " + insert1);
//
//    Movie movie2 = new Movie("it", "2017");
//    Movie insert2 = movieRepository.save(movie2);
//    LOGGER.info("New Movie is created: " + insert2);
//
//    Movie movie3 = new Movie("Bone Tomahawk", "2015");
//    Movie insert3 = movieRepository.save(movie3);
//    LOGGER.info("New Movie is created: " + insert3);
//
//    String filePath = "./data/processed_dataset_1000.csv";
//    persistMovies(filePath);
//
//    User user1 =
//        new User(
//            "MrBudweis123", "eriktheking77@gmail.com", "123456", "Ben", "Hodgins",
// "0151/2842343");
//
//    User insert4 = userRepository.save(user1);
//    LOGGER.info("New User is created: " + insert4);
//
//    User user2 =
//        new User(
//            "HotHolly",
//            "ebelchowitz@yahoo.de",
//            "3G8dh2FR6dgSwhd",
//            "Holly",
//            "Berrings",
//            "0173/2328462");
//
//    Rating rating1 = new Rating(4);
//    Rating rating2 = new Rating(5);
//    List<Rating> newRatings = new ArrayList<>();
//    User user3 =
//        new User(
//            "JämBäm99",
//            "höhlentroll23@web.de",
//            "HIUBIU123",
//            "Brittany",
//            "Tellko",
//            "0152/228535676");
//    rating1.setUser(user2);
//    rating2.setUser(user3);
//    rating1.setMovie(movie1);
//    rating2.setMovie(movie1);
//
//    newRatings.add(rating1);
//    newRatings.add(rating2);
//
//    movie1.setRatings(newRatings);
//    System.out.println("Ratings: " + movie1.getRatings());
//
//    movieRepository.save(movie1);
//
//    List<Movie> results = movieRepository.findUsersByKeyword("one");
//    System.out.println("result1");
//    System.out.println(results);
//
//    List<Movie> results2 = movieRepository.findByTitleContaining("one");
//    System.out.println("result2");
//    System.out.println(results2);
//  }
//
//  public void persistMovies(String filePath) {
//    ArrayList<String> moviesCsvFile = readFile(filePath);
//    ArrayList<Movie> movies = new ArrayList<>();
//    for (int i = 1; i < moviesCsvFile.size(); i++) {
//      List<String> movieProps = Arrays.asList(moviesCsvFile.get(i).split(","));
//      Movie movie = new Movie();
//      movie.setTitle(movieProps.get(0).toLowerCase());
//      movie.setYear(movieProps.get(1));
//      movies.add(movie);
//    }
//    List<Movie> blub = movieRepository.saveAll(movies);
//    LOGGER.info(blub.size() + " Movies were persisted!");
//  }
//
//  public ArrayList<String> readFile(String filePath) {
//    ArrayList<String> fileArrayList = new ArrayList<>();
//    try {
//      BufferedReader br = new BufferedReader(new FileReader(filePath));
//      String so;
//      while ((so = br.readLine()) != null) {
//        fileArrayList.add(so);
//      }
//      br.close();
//      return fileArrayList;
//    } catch (Exception e) {
//      e.getStackTrace();
//      return fileArrayList;
//    }
//  }
// }
