import { fireEvent, render, screen } from "@testing-library/react";
import PosterImage from "./PosterImage";
import { ObjectStorageImageSize } from "./imageUrls";

describe("PosterImage", () => {
  it("falls back to JPG when the WebP poster object is unavailable", () => {
    render(
      <PosterImage
        imageUrlToken="poster-token"
        size={ObjectStorageImageSize.Large}
      />,
    );

    const image = screen.getByAltText("movie poster") as HTMLImageElement;
    expect(image.src).toContain(
      "/imdb-clone/movies/posters/poster-token_size_600x900.webp",
    );

    fireEvent.error(image);

    expect(image.src).toContain(
      "/imdb-clone/movies/posters/poster-token_size_600x900.jpg",
    );
  });
});
