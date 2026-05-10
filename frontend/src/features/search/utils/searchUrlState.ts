export type SearchUrlState = {
  page: number;
  query: string | null;
};

export const parseSearchUrlState = (search: string): SearchUrlState => {
  const params = new URLSearchParams(search);
  const query = params.get("q") ?? params.get("query");
  const pageParam = Number.parseInt(params.get("page") ?? "1", 10);

  return {
    page: Number.isFinite(pageParam) && pageParam > 1 ? pageParam - 1 : 0,
    query: query?.trim() || null,
  };
};
