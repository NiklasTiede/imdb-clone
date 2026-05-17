# Deprecated Object Storage Dev Seed

This folder is kept only for historical reference. Local movie data and movie
media are now seeded through the versioned light/full seed Docker images built
from `infrastructure/movie-seed`.

Use the current local flow instead:

```bash
make docker-compose-dev-up
make seed-local-users
make seed-light SEED_VERSION=2026-05-17
```
