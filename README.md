# DoubtNest.AI — AI-Powered Smart Doubt Solver

DoubtNest.AI is a full-stack learning platform for school and college students. Ask an academic question (typed or **snapped from an image**), get a step-by-step AI explanation, publish it to a community feed by subject, discuss it with other students, and turn any doubt into a **10-question practice quiz**.

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot" />
  <img alt="React" src="https://img.shields.io/badge/React-19-61DAFB?logo=react" />
  <img alt="Vite" src="https://img.shields.io/badge/Vite-6-646CFF?logo=vite" />
  <img alt="Tailwind CSS" src="https://img.shields.io/badge/Tailwind%20CSS-4-38B2AC?logo=tailwindcss" />
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql" />
</p>

---

## ✨ Features

| Feature | Description |
| --- | --- |
| 🤖 **AI doubt solving** | Questions are answered by an LLM (Llama 3.1 via [Ollama](https://ollama.com)) with an academic-only system prompt and step-by-step explanations rendered as Markdown. |
| 📷 **Image-to-question (OCR)** | Upload a photo of a question; [Tesseract.js](https://tesseract.projectnaptha.com/) extracts the text in the browser. |
| 🗂️ **Categorised community** | Publish solved doubts under a subject category; browse the feed by category with pagination and sorting. |
| 💬 **Comments & upvotes** | Comment on doubts and upvote helpful answers. Comments are AI-moderated for relevance to the question. |
| 🏆 **Leaderboard** | Top 3 contributors ranked by total upvotes on their comments. |
| 📝 **Quiz Me** | Generate a 10-question multiple-choice quiz from any saved doubt. |
| 🔐 **Auth** | Email OTP verification on sign-up, BCrypt-hashed passwords, stateless JWT authentication. |
| 💳 **Pro subscription** | Razorpay checkout (₹99) upgrades a user from `normal` to `pro`. |

## 🏗️ Architecture

```
┌────────────────────────────┐   REST + JWT   ┌──────────────────────────────┐
│  Frontend (React + Vite)   │ ─────────────► │  Backend (Spring Boot, :5000)│
│  Tailwind · React Router   │                │  Spring Security · JPA       │
│  Tesseract.js (OCR)        │                │                              │
│  Razorpay Checkout         │                │   ├─► MySQL                  │
└────────────────────────────┘                │   ├─► Ollama (llama3.1)      │
                                              │   ├─► Gmail SMTP (OTP mail)  │
                                              │   └─► Razorpay Orders API    │
                                              └──────────────────────────────┘
```

## 📁 Project Structure

```
DoubtSolver/
├── backend/                      # Spring Boot REST API
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/project/doubt_solver/
│       │   ├── client/           # Feign client for Gemini API (not currently wired in)
│       │   ├── config/           # Security, JWT filter, CORS, pagination constants
│       │   ├── controller/       # REST endpoints
│       │   ├── exceptions/       # Global exception handling
│       │   ├── model/            # JPA entities: Users, Doubt, Category, Comment, EmailVerificationCode
│       │   ├── payloads/         # DTOs
│       │   ├── repository/       # Spring Data JPA repositories
│       │   └── service/          # Business logic (+ impl/)
│       └── resources/
│           ├── application.properties
│           ├── application-dev.properties
│           └── application-prod.properties
└── frontend/                     # React SPA
    ├── index.html
    ├── vite.config.js
    └── src/
        ├── auth/                 # Token/user helpers (localStorage)
        ├── context/              # AuthContext
        ├── components/           # Pages & UI (Home, Login, Register, AskDoubt, Pricing, ...)
        │   └── user-routes/      # Authenticated pages (Community, QuizList, QuizeMe, Profile, ...)
        └── services/             # Axios API clients
```

## 🚀 Getting Started

### Prerequisites

- **Java 21** (Maven wrapper is included)
- **Node.js 18+** and npm
- **MySQL 8**
- **[Ollama](https://ollama.com/download)** with the Llama 3.1 model
- A Gmail account with an [App Password](https://support.google.com/accounts/answer/185833) (for OTP emails)
- A [Razorpay](https://razorpay.com) test account (for payments)

### 1. Start the LLM

```bash
ollama pull llama3.1
ollama serve            # listens on http://localhost:11434
```

### 2. Create the database

```sql
CREATE DATABASE doubtsolver;
```

Tables are created automatically (`spring.jpa.hibernate.ddl-auto=update`). After you register and log in, create at least one category so doubts can be published:

```bash
curl -X POST http://localhost:5000/api/cat \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"catName": "Mathematics"}'
```

### 3. Run the backend

Set the required environment variables, then start the app:

```bash
cd backend

export APP_PASSWORD=<gmail-app-password>
export RAZORPAY_SECRET=<razorpay-key-secret>
export GEMINI_KEY=<optional-gemini-api-key>
export DATABASE_PASSWORD=<db-password>   # prod profile only

./mvnw spring-boot:run
```

The API runs on **http://localhost:5000**. The `dev` profile is active by default and connects to `localhost:3306/doubtsolver`; update [application-dev.properties](backend/src/main/resources/application-dev.properties) with your local MySQL credentials. Switch to production with `SPRING_PROFILES_ACTIVE=prod`.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The API base URL is set in [frontend/src/services/helper.js](frontend/src/services/helper.js).

### Docker (backend)

```bash
cd backend
docker build -t doubt-solver-backend .
docker run -p 5000:5000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_PASSWORD=... -e APP_PASSWORD=... -e RAZORPAY_SECRET=... \
  doubt-solver-backend
```

> The AI service calls Ollama at `http://localhost:11434`. Inside a container, `localhost` is the container itself, so Ollama has to be reachable from there (e.g. change the URL in `AIServiceImpl` to `host.docker.internal`).

## ⚙️ Configuration

| Property | Env var | Purpose |
| --- | --- | --- |
| `spring.datasource.*` | `DATABASE_PASSWORD` (prod) | MySQL connection |
| `spring.mail.username` / `spring.mail.password` | `APP_PASSWORD` | Gmail SMTP sender for OTP codes |
| `razorpay.key` | — | Razorpay key ID (backend) |
| `razorpay.secret` | `RAZORPAY_SECRET` | Razorpay key secret |
| `gemini.api.url` / `gemini.api.key` | `GEMINI_KEY` | Gemini API (Feign client present, currently unused) |
| `server.port` | — | `5000` |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `dev` (default) or `prod` |

Frontend settings:

- `BASE_URL` in [helper.js](frontend/src/services/helper.js): backend URL.
- Razorpay `key` in [payment.js](frontend/src/services/payment.js): must be the same key ID as `razorpay.key` on the backend.

## 📡 API Reference

All endpoints except those marked **Public** need an `Authorization: Bearer <token>` header (from `/api/auth/login`).

### Auth (Public)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/auth/send-code?email=` | Email a 6-digit OTP (valid for 5 minutes) |
| `POST` | `/api/auth/verify-code?email=&code=` | Verify the OTP |
| `POST` | `/api/auth/register` | Register a user |
| `POST` | `/api/auth/login` | Log in, returns a JWT (valid for 10 hours) |

### AI (Public)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/ai` | Body `{ "email", "question" }` returns the AI answer |
| `POST` | `/api/ai/generate-quiz` | Raw topic text in the body returns 10 MCQs `[{ question, optionA..D, answer }]` |

### Doubts

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/doubts/user/{userId}/category/{catId}` | Publish a doubt |
| `GET` | `/api/doubts` | All doubts (paginated) |
| `GET` | `/api/doubts/{doubtId}` | A single doubt |
| `GET` | `/api/doubts/user/{userId}` | A user's doubts (paginated) |
| `GET` | `/api/doubts/category/{catId}` | Doubts in a category (paginated) |
| `DELETE` | `/api/doubts/{doubtId}` | Delete a doubt |

Pagination query params: `pageNumber` (default `0`), `pageSize` (`6`), `sortBy` (`createdAt`), `sortDir` (`desc`).

### Comments

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/comments/user/{userId}/doubt/{doubtId}` | Add a comment (AI relevance check) |
| `PUT` | `/api/comments/upvote/{commentId}/user/{userId}` | Upvote a comment (once per user) |
| `GET` | `/api/comments/user/{userId}` | A user's comments |
| `DELETE` | `/api/comments/{commentId}` | Delete a comment |

### Categories

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/cat` | Create a category |
| `GET` | `/api/cat` | List categories |
| `GET` | `/api/cat/{catId}` | Get a category |
| `DELETE` | `/api/cat/{catId}` | Delete a category |

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/users` | List users |
| `GET` | `/api/users/{userId}` | Get a user |
| `GET` | `/api/users/leaders` | Top 3 users by comment upvotes |

### Payments (Public)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/payment/create-order` | Create a Razorpay order `{ username, amount }` |
| `PUT` | `/api/payment/make-user-pro/{userId}` | Upgrade the user to `pro` |

## 🖥️ Frontend Routes

| Route | Page |
| --- | --- |
| `/` | Landing page (hero, features, pricing) |
| `/login`, `/register` | Authentication (register includes email OTP) |
| `/private/ask-doubt` | Ask a question (text or image), get an AI answer, publish it |
| `/private/askdoubt-display` | Post-login dashboard |
| `/private/community`, `/private/community/:catId` | Community feed, optionally filtered by category |
| `/private/quiz-list` | Your saved doubts, ready to quiz on |
| `/private/quize-me/:doubtId` | Take an AI-generated quiz on a doubt |

## 🧰 Tech Stack

**Backend:** Spring Boot 3.5, Spring Security, JJWT 0.12, Spring Data JPA (Hibernate), MySQL, Spring WebFlux `WebClient` (Ollama), OpenFeign, Spring Mail, Razorpay Java SDK, ModelMapper, Lombok

**Frontend:** React 19, Vite 6, Tailwind CSS 4, React Router 6, Axios, Tesseract.js, react-markdown + remark-gfm, React Toastify, Lucide / React Icons, Day.js

## ⚠️ Known Limitations

- **JWT signing key is generated at startup**, so all users are logged out whenever the backend restarts. Load a fixed secret from configuration for production.
- **Free-tier limit is not enforced yet.** `AIServiceImpl` checks `doubtCount >= 100` for `normal` users, but `doubtCount` is never incremented.
- **Academic-topic filter is disabled.** `checkQuestion()` always returns `true`; the classifier prompt is commented out.
- **The pro upgrade is not verified server-side.** `/api/payment/*` is public and `make-user-pro` doesn't check the Razorpay payment signature.
- **The frontend API URL and Razorpay key are hard-coded**, not read from `.env` files.

## 🤝 Contributing

1. Fork the repo and create a branch: `git checkout -b feature/my-feature`
2. Commit your changes: `git commit -m "Add my feature"`
3. Push and open a Pull Request
