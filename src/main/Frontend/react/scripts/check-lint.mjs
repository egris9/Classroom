// Fails when the ESLint error count goes above the S0 baseline in
// docs/IMPLEMENTATION_PLAN.md. Lower BASELINE as slices clean errors up.
import { spawnSync } from "node:child_process";
import path from "node:path";

const BASELINE = 25;

const eslint = path.resolve(import.meta.dirname, "../node_modules/eslint/bin/eslint.js");
const run = spawnSync(process.execPath, [eslint, ".", "-f", "json"], {
    cwd: path.resolve(import.meta.dirname, ".."),
    encoding: "utf8",
    maxBuffer: 64 * 1024 * 1024,
});

let results;
try {
    results = JSON.parse(run.stdout);
} catch {
    console.error("ESLint did not produce JSON output:\n" + run.stderr);
    process.exit(2);
}

const errors = results.reduce((sum, file) => sum + file.errorCount, 0);
console.log(`ESLint errors: ${errors} (baseline ${BASELINE})`);
process.exit(errors > BASELINE ? 1 : 0);
