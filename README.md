# Classroom

A small web app for courses. A **Teacher** creates a course, uploads PDFs and shares an 8-character access code. **Students** join with the code, read the PDFs and ask a language model for a summary or, for the Teacher, a set of exercises. A Teacher's results are published to the course; a Student's stay private to that Student.

Roles belong to one course, not to an account: whoever creates a course is its Teacher, and whoever joins it with the code is a Student in that course only. A **Summary** is generated text about one PDF; an **Exercise set** is 5 generated questions with model answers for one PDF.

- Backend: Spring Boot 3, Java 21, MySQL, in `src/main/java`.
- Frontend: React, Vite and Tailwind, in `src/main/Frontend/react`.
- Model server: any server that speaks the OpenAI chat-completions API. Optional: without one the app uses a fake that returns canned text.

## Requirements

- JDK 21
- Node 22 and npm
- MySQL 8, with an empty database named `classroom`: `CREATE DATABASE classroom;`
- For real summaries: a model server (see below)

## Configuration

The backend reads environment variables, and also a `.env` file in the repo root (git-ignored). Copy [.env.example](.env.example) to `.env` and fill it in. A real environment variable wins over `.env`. Start the backend from the repo root so it finds the file.

| Variable | Default | Meaning |
|---|---|---|
| `JWT_SECRET` | none | **Required.** The app refuses to start without it. At least 32 characters; `openssl rand -hex 32` makes one. |
| `DB_URL` | `jdbc:mysql://localhost:3306/classroom` | JDBC url of the database |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | empty | Database password |
| `UPLOAD_DIR` | `uploads` | Folder for PDFs (`files/`) and profile pictures (`pictures/`). Created on startup. |
| `UPLOAD_MAX_SIZE` | `20MB` | Largest course PDF |
| `UPLOAD_MAX_PICTURE_SIZE` | `2MB` | Largest profile picture |
| `GENERATION_ADAPTER` | `fake` | `fake` or `local`. Without the three `GENERATION_*` lines of `.env.example` the adapter is `fake` and summaries are demo quotes of the PDF, not real summaries. |
| `GENERATION_BASE_URL` | empty | Model server url, for example `http://127.0.0.1:8081`. Required when the adapter is `local`. |
| `GENERATION_MODEL` | empty | Model name sent with each request |
| `GENERATION_TIMEOUT` | `120s` | Time allowed for one call to the model (for a streamed chat reply, the longest silence between two pieces) |
| `GENERATION_CHUNK_CHARS` | `10000` | Longest piece of text sent to the model at once |
| `GENERATION_MAX_CHUNKS` | `40` | A document with more pieces fails with `TOO_LONG` |
| `TOOLS_TRIAL_SALT` | none | **Required.** The app refuses to start without it. Anyone may use the AI tools once without an account; visitors are remembered only as hashes of their cookie and address mixed with this salt. Any long random text; `openssl rand -hex 32` makes one. Changing it gives every visitor a fresh free try. |
| `TOOLS_ANON_MAX_CHARS` | `20000` | Longest text a visitor with no account can send to the AI tools. A PDF is held to the same limit on its extracted text. |
| `TOOLS_ANON_MAX_PDF` | `5MB` | Largest PDF a visitor with no account can send to the AI tools |
| `TOOLS_MAX_CHARS` | `100000` | The same limit for a signed-in user (their PDF size limit is `UPLOAD_MAX_SIZE`) |
| `TOOLS_CHAT_MAX_MESSAGES` | `20` | Most messages in one chat. The client sends the whole conversation each time. |
| `TOOLS_CHAT_MAX_MESSAGE_CHARS` | `4000` | Longest single chat message |
| `TOOLS_TRIAL_TRUST_FORWARDED` | `false` | Set `true` only behind a proxy you run that sets `X-Forwarded-For`. Otherwise the header is ignored, because any caller can forge it. |

The frontend reads one variable at build time: `VITE_API_URL`, the backend url (default `http://localhost:8080`). The backend allows the origin `http://localhost:5175` only, which is the port the dev server uses.

## Run

Backend, from the repo root (`./mvnw` in Git Bash or a Unix shell, `.\mvnw` in PowerShell):

```
./mvnw spring-boot:run
```

It listens on port 8080 and creates its tables on first start.

Frontend:

```
npm --prefix src/main/Frontend/react install
npm --prefix src/main/Frontend/react run dev
```

Open <http://localhost:5175>. Sign up, create a course, upload a PDF, and choose Summarise.

## Model server

The default `fake` adapter needs nothing. For real output, start a server and switch the adapter. This is the setup the project was developed with, llama.cpp serving Qwen3.5 4B:

```
llama-server -hf unsloth/Qwen3.5-4B-GGUF:Q4_K_M --port 8081 -c 30208
```

Then in `.env`:

```
GENERATION_ADAPTER=local
GENERATION_BASE_URL=http://127.0.0.1:8081
GENERATION_MODEL=unsloth/Qwen3.5-4B-GGUF:Q4_K_M
```

Requests run one at a time. If the server is down, a request fails within the timeout with the code `MODEL_UNAVAILABLE`, shown on the file. To read what the model writes for your own PDFs, use the eval command in [eval/README.md](eval/README.md).

## Test and check

```
./mvnw test
npm --prefix src/main/Frontend/react run build
npm --prefix src/main/Frontend/react run lint
```

The backend tests use an in-memory H2 database, so they need no MySQL. GitHub Actions runs the same checks on every push.
