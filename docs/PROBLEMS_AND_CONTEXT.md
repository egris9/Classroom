# Classroom: problems and context

Snapshot of the repo at commit `2ff160a` plus uncommitted changes to `package.json`, `pom.xml` and `application.properties`.

Everything here comes from reading the source. The app was not run and nothing was tested, so the behaviours described are what the code says it does.

## 1. What the app is
A Google-Classroom-style app.
- **Backend:** Spring Boot 3.4, Java 21, MySQL, stateless JWT. About 1050 lines in `com.Classroom_ai.Classroom.{Cours, CourseFile, User}`.
- **Frontend:** React 18 + Vite + Tailwind in `src/main/Frontend/react`, on port 5175. About 1600 lines.
- **Repo:** GitHub remote `egris9/Classroom`, with pull requests merged from a second contributor's fork.
- **Flows:**
  - A user signs up and signs in.
  - They create a course, which gets an 8-character access code.
  - Others join with that code.
  - PDFs are uploaded to a course and viewed inline.
  - A `/tools` page offers AI summarisation and question generation.

## 2. Goals of the refactor
- Redesign the project end to end: design, logic, process, roles.
- Replace the dead external AI endpoints with a locally hosted model.
- Hardware: RTX 5060 laptop (8GB VRAM), Ryzen 9 HX CPU, 32GB RAM.
- Open choice: fine-tune a 2-3B model, or host a larger model as it is.

## 3. Provisional decisions
These come from one round of four multiple-choice questions. The design has not been grilled through, so treat them as starting positions.

| Topic | Position |
|---|---|
| AI seam | Browser -> Spring -> model server. The browser never talks to the model. |
| Model | Host first, fine-tune only if measured quality is not good enough. |
| Roles | Per course (teacher in one, student in another). |
| AI scope | Teachers generate and publish summaries/exercises for a course file. Students can run private summaries on files they can access. |

## 4. Problems found

### AI
1. The Java backend has no AI code. The browser posts PDFs to two hardcoded ngrok URLs (`src/api/AI_summarization.jsx:31`, `src/api/AI_test.jsx:28`). The user confirmed both URLs are dead, so neither AI feature works today.
2. The only trace of the old service is its contract: `POST /summarize` returns `{summary}` and `POST /generate-questions` returns `{questions: string[]}`. The service code, model and prompts are not in this repo.
3. There is no auth, config or persistence on the AI path. It is not linked to the PDFs already stored in a course, so users upload the file again each time.
4. Summarisation failures are only logged to the console. The user sees nothing.

### Roles and authorisation
5. There is no role field. "Teacher" means being `Course.teacher` and "student" means being in `Course.students`, but nothing checks either.
6. `/api/course-files/**` is `permitAll`. Without logging in, anyone can list a course's files by numeric course id and read any PDF by numeric file id.
7. Uploading needs only the course's access code, which every student has, and no login.
8. The frontend shows "Add a New PDF" to every viewer of a course.
9. No frontend route requires a login. `main.jsx` has no guards.
10. A teacher can join their own course as a student.

### Security
11. The JWT secret is hardcoded in `JwtTokenUtil.java:14`.
12. The DB root password is in plaintext in `application.properties`. That file is tracked and is not in `.gitignore`, so both secrets are in the pushed history.
13. Signin sends email and password as URL parameters (`api/auth.js:20-22`), so they can end up in logs.
14. Signup returns the `User` entity, including the password hash.
15. Both upload paths build the stored filename from the client's original filename. `CourseFileService.java:54-58` does no cleaning at all. A crafted filename could write outside the upload folder. Not tested.
16. `GET /api/auth/profile-picture/{fileName}` serves any file in the upload folder by name, and course PDFs live in that same folder. Names are UUID-prefixed, so they are hard to guess.

### Bugs in current behaviour
17. A wrong password returns 500, not 401. `UserService.authenticateUser` throws on failure, so the 401 branch in `UserController.java:147` is never reached and the global handler turns it into a 500.
18. An expired or invalid token returns 200 with the text "Invalid Token" (`JwtAuthFilter.java:49`). The frontend only reacts to 401, so after 24 hours the course pages break and the user is never sent to sign in.
19. That 401 handler redirects to `/login`, which is not a route. The sign-in route is `/signing`.
20. Course names must be unique across all teachers (`CourseService.java:55`). Only one course called "Math" can exist.
21. No upload size limit is configured, so Spring Boot's default of 1MB per file applies. Larger PDFs will be rejected with a 500.
22. `PDFUpload` ignores the course in its URL and makes the user type the access code. It also asks for a course name and a PDF name that it never sends.

### API design
23. Entities are used directly as request and response bodies (`Course` in, `User` out). There are no DTOs.
24. `/upload/{courseId}` takes an access code while `/course/{courseId}` takes a real id.
25. `GET /join` changes state.
26. `GET /courses?type=` requires `type`, ignores it, and returns hand-built `Map` objects.
27. There is no way to edit or delete a course or file, leave a course, or list a course's students.

### Files
28. Upload code exists twice: `CourseFileService.uploadFile` and `UserController.saveProfilePicture`.
29. `config/FileStorageConfig.java` is in a package Spring does not scan, so it never runs. The profile-picture path therefore never creates the upload folder.
30. The backend does not check file type.
31. The DB stores absolute paths, so moving the upload folder breaks every existing file. The uncommitted diff moves it.
32. `WebConfig` maps `/uploads/**` to `classpath:/static/uploads/`, which does not match `upload.dir`.

### Errors and structure
33. The global catch-all `@RestControllerAdvice` is nested inside `CourseService`. Controllers also have their own catch-alls. Messages mix French and English.
34. `System.out.println` is used instead of a logger.
35. The package is spelled `Cours`.

### Frontend
36. `http://localhost:8080` is hardcoded in many files.
37. `PDFUpload` and `PDFDisplay` call the backend without the token.
38. `App.jsx` and `api/Add_pdf.jsx` are unused.
39. The JWT is logged to the browser console on sign-in (`api/auth.js:24`).

### Build, tests, docs
40. The committed `pom.xml` lacks the security, validation and jjwt dependencies that committed code imports. Only the uncommitted diff adds them, so `HEAD` should not compile.
41. The only test is `contextLoads`, which needs a live MySQL.
42. There were no docs, standards or ADRs before this file. `claude.md` is empty.

## 5. Target design (draft)
Deep modules with small interfaces, using the vocabulary from the codebase-design skill. This is a first sketch and has not been challenged.

- **Membership (authorisation).** `roleOf(user, course) -> TEACHER | STUDENT | NONE`. The one place that decides per-course roles.
- **Course materials.** `put(course, upload)` and `open(file)`. It absorbs validation, naming, storage and streaming. The filesystem is the only adapter for now.
- **TextGeneration.** `summarize(text)` and `generateExercises(text, n)`. It hides PDF text extraction, chunking, prompts, timeouts and retries. Two adapters: the local model server and an in-memory fake for tests.
- **Generated content.** Summaries and exercise sets stored per `CourseFile`, with a published flag and the model version.
- **API layer.** DTOs, one error handler, ids instead of access codes, `POST /join`, JSON signin.

## 6. Hardware constraints
All figures are rough and need measuring on the actual laptop.
- **Inference.** A 4-bit 3-4B model takes about 2-3GB of VRAM. A 4-bit 7-8B model takes about 5GB and should also fit in 8GB with a modest context window. So the choice is wider than "2-3B or 5B".
- **Fine-tuning.** Memory depends mostly on sequence length, and summarisation has long inputs. QLoRA on a 2-3B model is realistic. Anything larger is tight and unmeasured.
- **Context.** Long PDFs need chunk-then-merge summarisation whatever the model.
- **Throughput.** One GPU serves one generation at a time, so requests need a queue or a time limit.
- **Availability.** If the laptop is the model server, the AI features only work while it is on.

## 7. Open questions
- Where is the original AI service (the notebook behind the ngrok URLs), and which model and prompts did it use? It is the quality baseline.
- What language are course materials in? The code comments and error messages are French. Small models vary a lot by language.
- Are the PDFs text-based or scanned? Scanned ones need OCR.
- Is there a dataset of PDF -> summary/exercises for fine-tuning, or must it be built?
- What is an "exercise": a list of open questions as today, or multiple choice with answers and grading?
- Should AI jobs be synchronous with a timeout, or queued with polling?
- Who else works on this repo, and is it a refactor in place or a rewrite on a new branch?
- Is the GitHub repo public? If so the DB password and JWT secret need rotating.
- Is this for a real deployment or a school project? That sets how far the security work should go.

## 8. Suggested order of work
1. Commit the `pom.xml` fix so `HEAD` builds.
2. Security and roles: secrets out of source, `Membership`, DTOs, locked-down file routes, correct 401s.
3. Course-materials module.
4. TextGeneration seam with the fake adapter, then the local model adapter.
5. Frontend API layer: one client, one base URL, token everywhere, route guards.
6. Spike: run candidate models on 5 real course PDFs before any fine-tuning.
