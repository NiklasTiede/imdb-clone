/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_IMDB_CLONE_BACKEND_ADDRESS?: string;
  readonly VITE_IMDB_CLONE_MINIO_ADDRESS?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
