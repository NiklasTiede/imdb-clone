package com.thecodinglab.imdbclone.catalog.internal.search;

import java.io.Serial;
import java.io.Serializable;

record MovieSearchProjectionTaskData(MovieSearchProjectionOperation operation)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
