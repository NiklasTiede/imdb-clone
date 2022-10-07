package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.exception.BadRequestException;

public class Pagination {

  public static final String DEFAULT_PAGE_NUMBER = "0";

  public static final String DEFAULT_PAGE_SIZE = "30";

  public static final int MAX_PAGE_SIZE = 30;

  public static void validatePageNumberAndSize(int page, int size) {
    if (page < 0) {
      throw new BadRequestException("Page number cannot be less than zero.");
    }
    if (size < 0) {
      throw new BadRequestException("Size number cannot be less than zero.");
    }
    if (size > MAX_PAGE_SIZE) {
      throw new BadRequestException("Page size must not be greater than " + MAX_PAGE_SIZE);
    }
  }
}
