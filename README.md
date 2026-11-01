# StudyFlow – Student Productivity & Study Management System

StudyFlow helps students organize academic work, plan study time, and track progress
in a personal workspace.

## Features

- Dashboard with task and study-session summaries
- Registration and sign-in using email or phone number
- Personal tasks and assignment tracking
- Study planner, calendar, and progress views
- In-app, browser, and Web Push notifications
- Profile, password, time-zone, and notification settings
- Dark and light themes
- Persistent local H2 database and a MySQL deployment profile

## Technologies

- Java 21
- Spring Boot 4.1
- Spring MVC, Spring Security, Spring Data JPA, and Bean Validation
- Maven
- H2 for local development; MySQL for deployments
- HTML, CSS, and vanilla JavaScript
- Web Push with VAPID

## Project structure

```text
.
├── backend/
│   ├── src/main/java/                 Spring Boot application and API
│   ├── src/main/resources/            Profiles, development migration, static assets
│   ├── src/test/                      Backend tests
│   ├── data/                          Local persistent H2 database (not committed)
│   ├── DATABASE.md                    Database and Web Push notes
│   ├── mvnw
│   └── mvnw.cmd
├── database/
│   ├── database.sql                   Database schema reference
│   └── migrations/                    Safe, incremental database migrations
└── frontend/                          HTML, CSS, JavaScript, and app assets
```

## Local setup

Install Java 21 or later. Maven is not required; the project includes the Maven
Wrapper.

From the project root, start the development application:

```cmd
cd backend
mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). With no profile override, the
application uses the `dev` profile and stores persistent H2 data in
`backend/data/studyflow.mv.db`. Keep this file in place between runs; it is local data
and is excluded from Git.

To run the backend test suite:

```cmd
cd backend
mvnw.cmd clean test
```

## Database configuration

- **Development:** The default `dev` profile uses a file-based H2 database. Set
  `STUDYFLOW_H2_PATH` only when intentionally selecting a different local database
  location. The H2 console is for local development only.
- **MySQL:** The `mysql` profile reads `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` from the process
  environment. It validates the schema rather than applying destructive Hibernate
  changes; provision the schema and apply the relevant migration before starting it.
- **Production security:** The `prod` profile enables secure session and CSRF cookies
  and disables the H2 console. Use it with `mysql` when serving the application over
  HTTPS.

See [backend/DATABASE.md](backend/DATABASE.md) for profile details, data-preservation
notes, and Web Push configuration. Web Push delivery requires `VAPID_PUBLIC_KEY`,
`VAPID_PRIVATE_KEY`, and `VAPID_SUBJECT`; keep the private key in a local environment
or deployment secret store, never in source control.
