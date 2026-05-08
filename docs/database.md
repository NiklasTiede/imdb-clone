# Database Migrations and Dataset Imports

Flyway owns database schema evolution. Application startup applies versioned migrations from
`src/main/resources/db/migration`.

The movie datasets and MinIO images are intentionally not Flyway migrations:

- Flyway migrations create and evolve tables, constraints, indexes, and small reference data like
  roles.
- The small development movie dataset stays in `src/main/resources/sql/2_init_data.sql` and can be
  loaded explicitly with `make seed-mysql-dev-data` when a developer wants local sample movies.
- The full IMDb dataset is produced by the infrastructure data-processing pipeline and imported
  separately after the schema has been migrated.
- MinIO images are seeded/imported by MinIO-specific scripts and assets, not by Flyway.

This keeps schema versioning independent from dataset versioning. A schema change should become a
new `V...__description.sql` migration. Dataset rebuilds should update the import pipeline or seed SQL
so newly imported rows match the current schema.

For existing local development databases that predate Flyway, the `dev` profile enables
`spring.flyway.baseline-on-migrate=true` at version `1`. That lets Flyway mark the old initial schema
as already present and then apply later schema migrations. Production should prefer an explicit
baseline step instead of automatic baselining.
