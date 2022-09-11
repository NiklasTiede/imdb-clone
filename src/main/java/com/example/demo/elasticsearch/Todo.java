package com.example.demo.elasticsearch;

public class Todo {

  private Long id;
  private Long userId;
  private String title;
  private boolean completed;

  public Todo() {}

  public Todo(Long id, Long userId, String title, boolean completed) {
    this.id = id;
    this.userId = userId;
    this.title = title;
    this.completed = completed;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  @Override
  public String toString() {
    return "Todo{"
        + ", userId="
        + userId
        + ", title='"
        + title
        + '\''
        + ", completed="
        + completed
        + '}';
  }
}
