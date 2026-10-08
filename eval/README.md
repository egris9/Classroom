# Eval

Runs real course PDFs through the local model, so a person can read what it writes and how long it takes.

1. Put the PDFs in `eval/pdfs/`. The folder is git-ignored. Use PDFs with a text layer; a scan is recorded as `NO_TEXT`.
2. Start the model server.
3. From the repo root:

   ```
   GENERATION_BASE_URL=http://127.0.0.1:8081 GENERATION_MODEL=<model name> ./mvnw test -Dtest=EvalRun -Deval=true
   ```

   On Windows PowerShell set the variables first: `$env:GENERATION_BASE_URL = "http://127.0.0.1:8081"`.

Each PDF becomes, in `eval/out/` (also git-ignored):

- `<name>.summary.txt`, the summary.
- `<name>.exercises.json`, 5 exercises (question and answer).
- one `results.md` with the text length, the status of each operation (`DONE` or a failure code) and the milliseconds each took.

A failure is recorded and the run goes on with the next PDF.

Optional variables: `GENERATION_TIMEOUT_SECONDS` (per request, default 300) and `GENERATION_CHUNK_CHARS` (default 10000).
