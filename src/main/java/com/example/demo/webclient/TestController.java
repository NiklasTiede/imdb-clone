package com.example.demo.webclient;

import com.example.demo.elasticsearch.Todo;
import com.example.demo.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

  private final TodoClient todoClient;

  public TestController(TodoClient todoClient) {
    this.todoClient = todoClient;
  }

  @GetMapping("/first")
  public ResponseEntity<String> getCurrentAccount(@RequestBody UserPrincipal userPrincipal) {

    Todo todo = new Todo();
    todo.setTitle("My Title");

    Todo resp = todoClient.getFirstEndpoint(todo);
    System.out.println(resp);

    return new ResponseEntity<>("empty", HttpStatus.OK);
  }
}
