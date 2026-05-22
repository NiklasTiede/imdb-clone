/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_VERSION?: string;
  readonly VITE_IMDB_CLONE_BACKEND_ADDRESS?: string;
  readonly VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS?: string;
  readonly VITE_OBSERVABILITY_CONSOLE?: string;
  readonly VITE_OBSERVABILITY_ENABLED?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
