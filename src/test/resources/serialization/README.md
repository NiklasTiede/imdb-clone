# Legacy scheduled-task fixture

`legacy-projection-upsert.bin` was generated before the Java package rename using the
existing `JavaSerializer` and a `MovieSearchProjectionTaskData(UPSERT)` instance from
`com.thecodinglab.imdbclone.catalog.internal.search.projection`.

It contains only the synthetic operation enum, no catalog/user data or credentials.
Keep the binary unchanged: regenerating it with `app.popcornsociety` classes would stop
testing compatibility with jobs already queued by the old backend.
