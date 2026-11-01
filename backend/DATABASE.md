# Database configuration

## Local development

The `dev` profile is the default when no profile is explicitly activated. StudyFlow is
the Student Productivity & Study Management System. Launch the application with `backend`
as the working directory. H2 stores data in
`backend/data/studyflow.mv.db` (with a companion lock file); the same file is reused on
each start. `STUDYFLOW_H2_PATH` can override the file base path, but keep it unchanged
between starts to reconnect to the same database. The H2 console is enabled only for
local development.

The existing development database path is intentionally unchanged. Keep
`backend/data/studyflow.mv.db` in place; do not delete or rename it when rebuilding or
restarting the application. Development uses Hibernate `update`, which retains existing
rows while applying compatible schema updates. The dev startup also converts an existing
H2-native `notifications.type` enum to `VARCHAR(30)`, preserving every notification row
while allowing new notification types; Hibernate `update` does not expand native enums.
This migration changes only the column type, not notification IDs, records, or ownership.

The dev startup also removes the former chat tables, dropping `chat_messages` before
`chat_conversations` to respect their foreign key. This migration does not alter or reset
users, tasks, study sessions, notifications, or other productivity data. For MySQL
deployments, apply `database/migrations/20261101_drop_chat_tables.sql` once before starting
the application; the production profile validates the schema and does not run dev
migrations.

An older application instance configured with in-memory H2 has a separate, temporary
database: its records are not automatically copied into the file database. If such an
instance still has records that need to be retained, export them before stopping it and
migrate them separately. This does not affect records already stored in the existing
file database.

## MySQL deployment

Activate the `railway` profile group for Railway, or activate both `prod` and `mysql`
for another HTTPS deployment. Set `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in the deployment
environment; never put credentials in configuration files. On Railway, reference the
managed MySQL service's `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, and
`MYSQLPASSWORD` variables. The application listens on the platform-provided `PORT`.

The MySQL profile uses HikariCP for connection pooling and runs Flyway migrations before
Hibernate validates the schema. `V1__create_initial_schema.sql` creates the application
tables and indexes only when absent; it does not truncate, reset, alter, or drop existing
tables. Existing nonempty MySQL schemas are baseline-marked at version 0 so the additive
initial migration can run; Hibernate validation then fails startup if an existing table
is incompatible. Resolve such schema differences with a separately reviewed migration;
do not disable validation or reset the database.

The existing local H2 database is not imported or modified by deployment. Migrating
local accounts or productivity records to Railway requires a separate export/import
plan and explicit confirmation.

Automated tests use an isolated in-memory H2 database and do not write to the development
database.

## Web Push configuration

Background Web Push also requires `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, and
`VAPID_SUBJECT` (for example, a `mailto:` contact). Generate a VAPID key pair locally with
`npx web-push generate-vapid-keys`; keep the private key out of source control, logs, and
chat. The public key alone is exposed to the authenticated browser to establish a
subscription.

For a local Windows CMD session, configure the variables in that same window before
starting the backend:

```cmd
set "VAPID_PUBLIC_KEY=your-public-key"
set "VAPID_PRIVATE_KEY=your-private-key"
set "VAPID_SUBJECT=mailto:you@example.com"
cd /d C:\path\to\Student-Productivity-System\backend
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Replace the example values locally; never paste credentials into chat or commit them.
`set` applies only to that CMD process tree. If using `setx` for persistent Windows user
variables instead, restart VS Code so its terminal inherits the new environment, then
restart StudyFlow. Web Push requires a secure browser context; `localhost` is supported,
but delivery to an external browser push service still requires working VAPID configuration
and network access.
