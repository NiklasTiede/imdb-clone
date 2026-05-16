/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_IMDB_CLONE_BACKEND_ADDRESS?: string;
  readonly VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
