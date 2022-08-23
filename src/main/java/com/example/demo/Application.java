package com.example.demo;

import com.example.demo.security.JwtAuthenticationFilter;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {

  public static void main(String[] args) {

    SpringApplication.run(Application.class, args);

    //    ConfigurableApplicationContext configurableApplicationContext =
    //        SpringApplication.run(Application.class, args);
    //
    //    MovieRepository movieRepository =
    // configurableApplicationContext.getBean(MovieRepository.class);
  }

  @PostConstruct
  void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    return new JwtAuthenticationFilter();
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
