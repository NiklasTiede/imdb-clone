export type CommentAuthor = {
  displayName?: string;
  id?: number;
  imageUrlToken?: string;
  username?: string;
};

export type MovieComment = {
  accountId?: number;
  createdAtInUtc?: string;
  id?: number;
  message?: string;
  modifiedAtInUtc?: string;
  movieId?: number;
};

export type MovieCommentsPage = {
  content?: MovieComment[];
  last?: boolean;
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
};
