# Spring Boot Invoice API

A REST API for managing customers and invoices, with JWT authentication. Built as a portfolio project to show production-style Spring Boot backend work.

**Stack:** Java 21 · Spring Boot 4.1 · Spring Security (JWT) · Spring Data JPA · PostgreSQL / H2 · Swagger (OpenAPI) · JUnit + MockMvc · Docker

## Features
- Register / login with BCrypt-hashed passwords and stateless JWT tokens
- Customers and invoices CRUD, with line items and computed totals
- Invoice status workflow: `DRAFT → SENT → PAID`, filter by status
- Dashboard summary endpoint (paid vs outstanding)
- **Per-user data isolation**: every query is scoped to the logged-in user (tested)
- Request validation with clear 400 errors
- Interactive API docs at `/swagger-ui.html`
- Health check at `/actuator/health`

## Run locally
```bash
export JWT_SECRET=$(openssl rand -hex 32)   # required, the app will not start without it
./mvnw spring-boot:run
```
Uses an in-memory H2 database, no setup needed. Open http://localhost:8080/swagger-ui.html

## Run tests
```bash
mvn test
```

## Run with Docker
```bash
docker build -t invoice-api .
docker run -p 8080:8080 -e JWT_SECRET=$(openssl rand -hex 32) invoice-api
```

## Deploy (free, no credit card)
1. **Database:** create a free Postgres on [Neon](https://neon.tech) and copy its connection string (`postgresql://USER:PASSWORD@HOST/DB?sslmode=require`). Paste it as `DATABASE_URL` unchanged.
2. **API:** on [Render](https://render.com) choose *New → Blueprint* and pick this repo. `render.yaml` creates a free Docker web service and a random `JWT_SECRET`.
3. Set `CORS_ORIGINS` to your dashboard's URL.

Free Render services sleep after 15 minutes idle; the first request then takes about a minute.

## Configuration (environment variables)
| Variable | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://host/db?sslmode=require` | in-memory H2 |
| `DATABASE_USER` / `DATABASE_PASSWORD` | DB credentials | `sa` / empty |
| `JWT_SECRET` | HMAC key, 32+ characters. **Required** | none |
| `CORS_ORIGINS` | Allowed frontend origins, comma-separated | `http://localhost:3000` |

## API overview
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account, returns JWT |
| POST | `/api/auth/login` | Log in, returns JWT |
| GET/POST | `/api/customers` | List / create customers |
| PUT/DELETE | `/api/customers/{id}` | Update / delete |
| GET/POST | `/api/invoices?status=PAID` | List (optional filter) / create |
| GET | `/api/invoices/summary` | Totals paid and outstanding |
| PATCH | `/api/invoices/{id}/status` | Change status |

All routes except `/api/auth/**` need `Authorization: Bearer <token>`.

## Project structure
```
auth/      JWT issuing + verification, register/login
config/    Spring Security filter chain, CORS
customer/  Customer entity, repository, controller
invoice/   Invoice entity with embedded line items, controller
```
