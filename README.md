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
- For scans and PDFs with garbled text: Tesseract 5 (optional, see [OCR](#ocr))

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
| `GENERATION_OCR_ADAPTER` | `none` | `none` or `tesseract`. `none` switches OCR off: a PDF is read from its own text only. See [OCR](#ocr). |
| `GENERATION_OCR_COMMAND` | `tesseract` | The Tesseract program, for example `C:/Program Files/Tesseract-OCR/tesseract.exe`. Used only when the adapter is `tesseract`. |
| `GENERATION_OCR_LANGUAGES` | `ara+fra+eng` | Languages Tesseract reads, joined with `+`. Each needs its language data installed. Fewer languages is faster. |
| `GENERATION_OCR_DPI` | `300` | Resolution of the page images given to OCR. Lower is faster and less exact. |
| `GENERATION_OCR_PAGE_TIMEOUT` | `60s` | Time allowed for one page. A page that takes longer fails the file with `OCR_FAILED`. |
| `GENERATION_OCR_MAX_PAGES` | `80` | A PDF with more pages is not given to OCR and fails with `TOO_LONG` |
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

### Pages

| Route | Who | What |
|---|---|---|
| `/` | anyone | Landing page (signed in: shows My courses) |
| `/signin`, `/signup` | anyone | Sign in, create an account |
| `/tools` | anyone | AI study tools: summary, exercises and a streaming chat. A visitor with no account gets one use, then is asked to sign up (see `TOOLS_*` above). |
| `/courses` | signed in | My courses: Teaching and Enrolled, Add course (create or join with a code) |
| `/courses/:courseId` | members | Course: Materials, People, Settings (cover picture, delete or leave) |
| `/courses/:courseId/files/:fileId` | members | A PDF beside its Summary and Exercises |
| anything else | anyone | Not found page |

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

## OCR

Optional, and off by default. The backend reads a PDF's own text. Two kinds of PDF have none it can use: a scan (pages that are pictures), and a PDF whose fonts do not say which letters they draw, which is common in Arabic documents and comes out as garbled text. With OCR off, the first fails with `NO_TEXT` and the second with `UNREADABLE_TEXT`, shown on the file. With OCR on, those two kinds are read from the images of their pages instead. A PDF with readable text is never given to OCR.

To switch it on, install [Tesseract 5](https://github.com/tesseract-ocr/tesseract) with the language data for Arabic, French and English (on Windows, the UB Mannheim installer, with Arabic and French ticked under additional language data), then in `.env`:

```
GENERATION_OCR_ADAPTER=tesseract
GENERATION_OCR_COMMAND=C:/Program Files/Tesseract-OCR/tesseract.exe
```

To switch it off again, remove those lines or set `GENERATION_OCR_ADAPTER=none`, and restart the backend. Pages already read stay stored, so files read by OCR keep working.

What it costs: Tesseract runs on the CPU only, one page at a time, a few seconds per page. A file is read once: its page texts are stored, and every later summary or exercise set uses them. A PDF over `GENERATION_OCR_MAX_PAGES` is refused before any OCR. The AI tools page (`/tools`) never uses OCR. With OCR on, the backend refuses to start if the program or one of the languages is missing.

## Test and check

```
./mvnw test
npm --prefix src/main/Frontend/react run build
npm --prefix src/main/Frontend/react run lint
```

The backend tests use an in-memory H2 database, so they need no MySQL. GitHub Actions runs the same checks on every push.
