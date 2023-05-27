package com.thecodinglab.imdbclone.utility;

import java.util.ArrayList;
import java.util.List;

public class PartitionList {

  private PartitionList() {}

  public static <T> List<List<T>> partition(List<T> list, int chunkSize) {
    List<List<T>> partitions = new ArrayList<>();
    for (int j = 0; j < list.size(); j += chunkSize) {
      partitions.add(list.subList(j, Math.min(list.size(), j + chunkSize)));
    }
    return partitions;
  }
}
