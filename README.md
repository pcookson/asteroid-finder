# Asteroid Hunter Monorepo

Monorepo with:

- `server`: Spring Boot API (Java 21, Maven Wrapper)
- `web`: Vite + React + TypeScript frontend

## Project Structure

```text
.
├── server
└── web
```

## Prerequisites

- Java 21 installed (`java -version`)
- Node.js + npm installed (`node -v`, `npm -v`)

Notes:

- `server` uses `./mvnw`, so a global Maven install is optional.
- First runs of `./mvnw` and `npm install` download dependencies from the internet.
- Copy env templates before running (not required by code yet, but ready for future deploy config):
  - `cp server/.env.example server/.env`
  - `cp web/.env.example web/.env`
  - These files are examples only; no secrets are committed in this repo.

## Run The Backend (`server`)

```bash
cd server
./mvnw test
./mvnw spring-boot:run
```

Default port is `8080` and is configurable via `PORT`:

```bash
cd server
PORT=8081 ./mvnw spring-boot:run
```

### Backend Endpoints

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/config
```

Expected responses:

- `/api/health` -> `{"status":"ok"}`
- `/api/config` -> `{"timezone":"..."}`

## Run The Frontend (`web`)

```bash
cd web
npm install
npm run dev
```

Open:

- `http://localhost:5173`

The Vite dev server proxies `/api/*` requests to `http://localhost:8080`.

## Deploy Later

No deployment steps are configured yet.

- `web` can be deployed to Vercel later.
- `server` can be deployed to Render later.
- `server` supports the `PORT` environment variable and includes a `Dockerfile` for container-based deployment.

## Full Local Dev Workflow

1. Start backend:
   ```bash
   cd server
   ./mvnw spring-boot:run
   ```
2. In a second terminal, start frontend:
   ```bash
   cd web
   npm install
   npm run dev
   ```
3. Open `http://localhost:5173`
4. Verify UI shows `API Status: ok`

If the backend is stopped, the UI should show `API Status: error` and a short error message.

## Optional Makefile Shortcuts

For local development convenience (does not affect deployment), you can use the root `Makefile`:

```bash
make help
make server
make web-install
make web
```

Useful targets:

- `make server` - run backend
- `make server-test` - run backend tests
- `make web-install` - install frontend dependencies
- `make web` - run frontend dev server
- `make web-build` - build frontend

## If `npm install` Fails Due To Permissions

Do not use `sudo npm install` in the project.

Common fixes:

### Option 1 (recommended): Use a Node version manager

Install and use `nvm` (or `fnm`) so npm writes to your user directory, not system paths.

Example with `nvm` (after installing `nvm`):

```bash
nvm install --lts
nvm use --lts
cd web
npm install
```

### Option 2: Fix npm global directory ownership (if a previous sudo install caused issues)

Check the npm cache path:

```bash
npm config get cache
```

If it points into your home directory and ownership is wrong, fix ownership:

```bash
sudo chown -R "$(whoami)" ~/.npm
```

Then retry:

```bash
cd web
npm install
```

### Option 3: Per-command cache directory workaround

```bash
cd web
npm install --cache /tmp/npm-cache
```

## Troubleshooting

### Port `8080` already in use

Find and stop the process:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
kill <PID>
```

Or run backend on a different port:

```bash
cd server
PORT=8081 ./mvnw spring-boot:run
```

If you change backend port, update the frontend proxy in `web/vite.config.ts` (or switch it back later).
