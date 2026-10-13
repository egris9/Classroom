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

- `<name>.summary.short.txt`, `.summary.medium.txt`, `.summary.long.txt` and `.summary.extra_long.txt`, the summary at each level.
- `<name>.exercises.json`, 5 exercises (question and answer).
- one `results.md` with the text length, and for each summary level and for the exercises the status (`DONE` or a failure code) and the milliseconds it took; each summary also has its word count.

A failure is recorded and the run goes on with the next PDF.

Each call for a summary also prints a line with its level, the tokens the model wrote and the milliseconds (`Summary call (whole, level LONG): 612 completion tokens in 14200 ms`). The token caps and the long time limit of the app are set from these lines.

Optional variables: `GENERATION_TIMEOUT_SECONDS` (per request, default 300), `GENERATION_LONG_TIMEOUT_SECONDS` (per request of a long or extra long summary, default 600), `GENERATION_SUMMARY_MAX_TOKENS` (the token caps in level order, default `400,900,1550,2450`; set them high, for example `4000,4000,4000,4000`, to measure what the model writes when nothing cuts it) and `GENERATION_CHUNK_CHARS` (default 10000).

OCR is off in the eval unless `OCR_COMMAND` names the Tesseract program, for example `OCR_COMMAND="C:/Program Files/Tesseract-OCR/tesseract.exe"`. With it, a scan or a PDF with garbled text is read from its page images, as the app does with `GENERATION_OCR_ADAPTER=tesseract`; without it they are recorded as `NO_TEXT` and `UNREADABLE_TEXT`. `OCR_LANGUAGES` (default `ara+fra+eng`) and `OCR_DPI` (default 300) are optional.
