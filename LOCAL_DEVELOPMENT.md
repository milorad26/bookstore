# Local development

The backend uses Spring's `local` profile by default. It uses the file-backed H2 database at `bookstore/data/bookstore`, a local-only development JWT signing key, and permits the Vite frontend at `http://localhost:5173`. Stripe checkout requires your own Stripe test keys: set `STRIPE_SECRET_KEY` and `STRIPE_PUBLISHABLE_KEY` in the backend process environment before starting it. Keep those keys out of source control and do not reuse the previously committed key.

Start the backend from `bookstore` with `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). Start the frontend from `bookstore-frontend` with `npm run dev`. The Vite proxy forwards `/api` requests to `http://localhost:8080`.

For production, set `SPRING_PROFILES_ACTIVE` to a production profile, and supply `JWT_SECRET` (a unique random secret), `CORS_ALLOWED_ORIGINS`, `DATABASE_URL`, `DATABASE_DRIVER`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and the Stripe keys through the deployment environment or secret manager. Never reuse the local development key. The H2 console is available only in the local profile. Use the same shared database across backend instances so revocations are visible to every instance.
