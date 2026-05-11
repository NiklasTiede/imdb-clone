# Frontend Data Contract

This file summarizes the data currently exposed by the backend for UI design
work. The canonical source is `frontend/src/client/imdb-clone-backend.yaml`;
this document is the human-readable version to hand to design tools or Claude.

Use this contract to design UI around the data that exists today. Add future
ideas to separate specs until the backend exposes them.

## Global Notes

- Base URL in local development: `http://localhost:8080`
- Pagination shape:

```ts
type PagedResponse<T> = {
  content?: T[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
  last?: boolean;
};
```

- Backend max page size is `30`. Do not request `size > 30`.
- Image fields are tokens, not full URLs. Frontend builds MinIO image URLs from
  `imageUrlToken`.
- Authenticated writes use the JWT access token from login.
- Public reads currently include movies, search, comments, public profiles,
  public watchlists, and public ratings.

## Movie

Source DTO: `MovieRecord`

```ts
type Movie = {
  id?: number;
  primaryTitle?: string;
  originalTitle?: string;
  startYear?: number;
  endYear?: number;
  runtimeMinutes?: number;
  modifiedAtInUtc?: string;
  createdAtInUtc?: string;
  movieGenre?: MovieGenre[];
  movieType?: MovieType;
  imdbRating?: number;
  imdbRatingCount?: number;
  adult?: boolean;
  rating?: number;
  ratingCount?: number;
  description?: string;
  imageUrlToken?: string;
};
```

Use for:
- search result cards
- movie detail hero
- synopsis
- poster grids
- watchlist and ratings pages when embedded or fetched by IDs

Important UI fields:
- `primaryTitle`: display title
- `startYear`: release year
- `runtimeMinutes`: runtime label and stats
- `movieGenre`: genre chips or filters
- `movieType`: movie/video/series label
- `imdbRating`, `imdbRatingCount`: IMDb/community rating display
- `rating`, `ratingCount`: app/user rating aggregate if present
- `description`: synopsis text
- `imageUrlToken`: poster image

Enums:

```ts
type MovieGenre =
  | "HORROR" | "MYSTERY" | "THRILLER" | "CRIME" | "WESTERN" | "WAR"
  | "ACTION" | "ADVENTURE" | "FAMILY" | "COMEDY" | "ANIMATION"
  | "FANTASY" | "SCI_FI" | "DRAMA" | "ROMANCE" | "SPORT" | "HISTORY"
  | "BIOGRAPHY" | "MUSIC" | "MUSICAL" | "DOCUMENTARY" | "NEWS"
  | "ADULT" | "REALITY_TV" | "TALK_SHOW" | "GAME_SHOW"
  | "FILM_NOIR" | "SHORT";

type MovieType =
  | "SHORT" | "MOVIE" | "VIDEO" | "TV_MOVIE" | "TV_EPISODE"
  | "TV_MINI_SERIES" | "TV_SPECIAL" | "TV_SERIES" | "TV_SHORT"
  | "TV_PILOT" | "VIDEO_GAME";
```

Endpoints:

```http
GET /api/movie/{movieId}
POST /api/movie/get-movies?page=0&size=30
POST /api/search/movies?query={query}&page=0&size=30
GET /api/movie/search/{primaryTitle}?page=0&size=30
```

Search filter body:

```ts
type MovieSearchRequest = {
  minStartYear?: number;
  maxStartYear?: number;
  minRuntimeMinutes?: number;
  maxRuntimeMinutes?: number;
  movieGenre?: MovieGenre[];
  movieType?: MovieType;
};
```

Example:

```json
{
  "id": 2872718,
  "primaryTitle": "Nightcrawler",
  "originalTitle": "Nightcrawler",
  "startYear": 2014,
  "runtimeMinutes": 117,
  "movieGenre": ["CRIME", "DRAMA", "THRILLER"],
  "movieType": "MOVIE",
  "imdbRating": 7.8,
  "imdbRatingCount": 625000,
  "rating": 8.4,
  "ratingCount": 12,
  "description": "A driven freelancer enters the world of crime journalism.",
  "imageUrlToken": "nightcrawler-poster-token"
}
```

## Watchlist

Source DTO: `WatchedMovieRecord`

```ts
type WatchlistItem = {
  accountId?: number;
  movieId?: number;
  addedAt?: string;
  movie?: Movie;
};
```

Use for:
- watchlist poster grid
- watchlist stats
- "Added X days ago"
- remove from watchlist
- pick-for-me dialog

Endpoints:

```http
GET /api/account/{username}/watchlist?page=0&size=30
PUT /api/watched-movie/{movieId}/watch
DELETE /api/watched-movie/{movieId}
```

Auth:
- `GET /api/account/{username}/watchlist` is public.
- `PUT` and `DELETE` require `ROLE_USER`.

Example:

```json
{
  "accountId": 2,
  "movieId": 2872718,
  "addedAt": "2026-05-08T12:00:00Z",
  "movie": {
    "id": 2872718,
    "primaryTitle": "Nightcrawler",
    "startYear": 2014,
    "runtimeMinutes": 117,
    "movieGenre": ["CRIME", "DRAMA", "THRILLER"],
    "movieType": "MOVIE",
    "imdbRating": 7.8,
    "imageUrlToken": "nightcrawler-poster-token"
  }
}
```

Current limitations:
- No watched/done status on watchlist items yet.
- No backend watchlist stats endpoint yet; current frontend computes stats from
  loaded items.

## Ratings

Source DTO: `RatingRecord`

```ts
type Rating = {
  rating?: number;
  accountId?: number;
  movieId?: number;
};
```

Use for:
- user's ratings page
- movie detail "your rating"
- rating/unrating actions

Endpoints:

```http
GET /api/account/{username}/ratings?page=0&size=30
PUT /api/movie-rating/{movieId}/rating-score/{score}
DELETE /api/movie-rating/{movieId}
```

Auth:
- `GET /api/account/{username}/ratings` is public.
- `PUT` and `DELETE` require `ROLE_USER`.

Example:

```json
{
  "rating": 8,
  "accountId": 2,
  "movieId": 2872718
}
```

Current limitations:
- Rating records do not embed movie data. The frontend must fetch movies by
  `movieId` to render posters/titles on the ratings page.

## Comments

Source DTO: `CommentRecord`

```ts
type Comment = {
  id?: number;
  message?: string;
  accountId?: number;
  movieId?: number;
  createdAtInUtc?: string;
};
```

Use for:
- movie detail comment section
- user profile comment history

Endpoints:

```http
GET /api/comment/{commentId}
GET /api/comment/{movieId}/comments?page=0&size=30
GET /api/account/{username}/comments?page=0&size=30
POST /api/comment/{movieId}
PUT /api/comment/{commentId}
DELETE /api/comment/{commentId}
```

Auth:
- `GET` endpoints are public.
- `POST`, `PUT`, and `DELETE` require `ROLE_USER`.

Create/update body:

```ts
type CommentRequest = {
  message?: string; // max 1000 chars
};
```

Example:

```json
{
  "id": 101,
  "message": "This movie has a great final act.",
  "accountId": 2,
  "movieId": 2872718,
  "createdAtInUtc": "2026-05-08T12:00:00Z"
}
```

Current limitations:
- Comment records do not expose username/avatar.
- Comment records do not embed movie data.

## Account Profiles

Public profile DTO: `PublicAccountProfile`

```ts
type PublicAccountProfile = {
  username?: string;
  firstName?: string;
  lastName?: string;
  bio?: string;
  imageUrlToken?: string;
  ratingsCount?: number;
  watchlistCount?: number;
  commentsCount?: number;
};
```

Current account profile DTO: `AccountProfile`

```ts
type AccountProfile = {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  bio?: string;
  birthday?: string;
  imageUrlToken?: string;
  ratingsCount?: number;
  watchlistCount?: number;
  commentsCount?: number;
};
```

Use for:
- account settings
- profile header
- public user profile pages
- app bar/profile avatar

Endpoints:

```http
GET /api/account/{username}/profile
GET /api/account/me
GET /api/account/me/profile
PUT /api/account/{username}
DELETE /api/account/{username}
```

Auth:
- Public profile endpoint is public.
- `/api/account/me`, `/api/account/me/profile`, update, and delete require
  `ROLE_USER`.

Update body:

```ts
type AccountRecord = {
  username?: string;
  password?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  bio?: string;
  birthday?: string; // YYYY-MM-DD
};
```

Current UI rule:
- Treat `username` and `email` as read-only for now. Backend update currently
  updates profile fields like `firstName`, `lastName`, `phone`, `birthday`,
  and `bio`, but email/username changes need a deliberate confirmation flow.

Example:

```json
{
  "username": "les_grossman",
  "email": "les@example.com",
  "firstName": "Les",
  "lastName": "Grossman",
  "bio": "Movie fan.",
  "birthday": "1975-04-01",
  "imageUrlToken": "profile-photo-token",
  "ratingsCount": 12,
  "watchlistCount": 8,
  "commentsCount": 3
}
```

## Authentication

Endpoints:

```http
POST /api/auth/login
POST /api/auth/registration
GET /api/auth/check-username-availability?username={username}
GET /api/auth/check-email-availability?email={email}
GET /api/auth/confirm-email-address?token={token}
GET /api/auth/reset-password?email={email}
POST /api/auth/save-new-password
```

Login body/response:

```ts
type LoginRequest = {
  usernameOrEmail: string;
  password: string;
};

type LoginResponse = {
  accessToken?: string;
  tokenType?: string;
};
```

Registration body:

```ts
type RegistrationRequest = {
  username: string;
  email: string;
  password: string;
};
```

Availability response:

```ts
type UserIdentityAvailability = {
  isAvailable?: boolean;
};
```

## File Storage

Endpoints:

```http
POST /api/file-storage/profile-photo
DELETE /api/file-storage/profile-photo
POST /api/file-storage/movie/{movieId}
DELETE /api/file-storage/movie/{movieId}
```

Auth:
- Profile photo upload/delete requires `ROLE_USER`.
- Movie image upload/delete should be treated as admin/tooling functionality.

Use for:
- profile avatar upload/remove
- admin movie poster management

Response:
- Upload endpoints return string arrays with stored image names/tokens.
- Delete endpoints return `MessageResponse`.

## Admin/Tooling Endpoints

These exist in the backend but should not drive normal user UI yet.

```http
POST /api/account/add-account
PUT /api/account/{username}/give-admin
PUT /api/account/{username}/take-admin
POST /api/movie/create-movie
PUT /api/movie/{movieId}
DELETE /api/movie/{movieId}
```

Auth:
- Admin endpoints require admin/user authorization according to backend method
  security.

## Current UI Opportunities

Good pages to design with current data:
- Home/search page using `Movie`
- Search result grid using `PagedResponse<Movie>`
- Movie detail page using `Movie`, comments, current rating, watchlist status
- Watchlist page using `WatchlistItem`
- Ratings page using `Rating` plus fetched `Movie`
- Account settings using `AccountProfile`
- Public profile using `PublicAccountProfile`, comments, ratings, watchlist

Data missing for richer future UI:
- comment author username/avatar
- embedded movie data on rating records
- backend watchlist/rating status endpoint per movie
- social activity feed
- follow/friend graph
- recommendations/similar movies
- notification/messages data
