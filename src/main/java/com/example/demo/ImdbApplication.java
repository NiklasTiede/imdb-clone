package com.example.demo;

import com.example.demo.repository.MovieRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ImdbApplication {

  public static void main(String[] args) {

    ConfigurableApplicationContext configurableApplicationContext =
        SpringApplication.run(ImdbApplication.class, args);

    MovieRepository movieRepository = configurableApplicationContext.getBean(MovieRepository.class);
  }
}
