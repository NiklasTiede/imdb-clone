package com.thecodinglab.imdbclone.shared.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public class PagedResponse<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean last;

  public PagedResponse() {}

  public PagedResponse(
      List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
    this.last = last;
  }

  public static <T> PagedResponse<T> from(Page<T> page) {
    return new PagedResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }

  public <R> PagedResponse<R> map(Function<T, R> mapper) {
    return new PagedResponse<>(
        content.stream().map(mapper).toList(), page, size, totalElements, totalPages, last);
  }

  public List<T> getContent() {
    return content;
  }

  public void setContent(List<T> content) {
    this.content = content;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public long getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(long totalElements) {
    this.totalElements = totalElements;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }

  public boolean isLast() {
    return last;
  }

  public void setLast(boolean last) {
    this.last = last;
  }
}
