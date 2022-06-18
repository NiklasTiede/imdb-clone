// @filename: models.ts
import { Models } from "@rematch/core"
import { movies } from "./movies"

export interface RootModel extends Models<RootModel> {
  movies: typeof movies
}

export const models: RootModel = { movies }
