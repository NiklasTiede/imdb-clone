import { expect, test } from "@playwright/test";

const transparentPixel =
  "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAqf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/ASP/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/ASP/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAY/Aqf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/ISP/2gAMAwEAAgADAAAAEP/EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QH//EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QH//EFBABAQAAAAAAAAAAAAAAAAAAABD/2gAIAQEAAT8QH//Z";

test("searches movies and renders a seeded poster", async ({ page }) => {
  await page.route("**/api/search/movies**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [
          {
            id: 2872718,
            movieType: "MOVIE",
            primaryTitle: "Nightcrawler",
            originalTitle: "Nightcrawler",
            adult: false,
            startYear: 2014,
            endYear: null,
            runtimeMinutes: 117,
            movieGenre: ["CRIME", "DRAMA", "THRILLER"],
            imdbRating: 7.8,
            imdbRatingCount: 528339,
            description:
              "A driven freelancer enters the world of crime journalism.",
            imageUrlToken: "9BGAIYNfdY90aIkV66dIJ6Olee7JGn",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        last: true,
      }),
    });
  });

  await page.route("**/imdb-clone/movies/*.jpg", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });

  await page.goto("/");
  await expect(page.getByText("Home Page")).toBeVisible();

  await page.getByRole("textbox", { name: "search" }).fill("Nightcrawler");
  await page.getByRole("textbox", { name: "search" }).press("Enter");

  await expect(page).toHaveURL(/\/movie-search\?query=Nightcrawler$/);
  await expect(page.getByRole("link", { name: "Nightcrawler" })).toBeVisible();
  await expect(page.getByAltText("movie poster")).toHaveAttribute(
    "src",
    /9BGAIYNfdY90aIkV66dIJ6Olee7JGn_size_120x180\.jpg/,
  );
});
