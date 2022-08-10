package com.example.demo.controller;

import com.example.demo.dto.MovieRecord;
import com.example.demo.dto.Todo;
import com.example.demo.dto.mapper.MovieMapper;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.MovieRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class AnotherController {

  private final MovieRepository movieRepository;

  private static final Logger logger = Logger.getLogger(String.valueOf(AnotherController.class));

  private final MovieMapper movieMapper;

  public AnotherController(MovieRepository movieRepository, MovieMapper movieMapper) {
    this.movieRepository = movieRepository;
    this.movieMapper = movieMapper;
  }

  @GetMapping("/simple")
  public ResponseEntity<MovieRecord> simple() {
    Movie movie = movieRepository.findById(1457767L).get();
    MovieRecord movieRecord = movieMapper.entityToDTO(movie);
    return new ResponseEntity<>(movieRecord, HttpStatus.OK);
  }

  @GetMapping("/todo/{id}")
  public ResponseEntity<Todo> json2(@PathVariable("id") String id) {
    Mono<Todo> employeeMono =
        WebClient.create("https://jsonplaceholder.typicode.com/todos/" + id)
            .get()
            .retrieve()
            .bodyToMono(Todo.class)
            .share();
    Todo todo = employeeMono.share().block();
    return new ResponseEntity<>(todo, HttpStatus.OK);
  }

  @GetMapping(value = "/json")
  public Map<String, String> json() {
    Map<String, String> rightHereMap = new HashMap<>();
    rightHereMap.put("key1", "value1");
    rightHereMap.put("key2", "value2");
    return rightHereMap;
  }
}
