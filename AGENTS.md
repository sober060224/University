# AGENTS.md

Repository guidance for AI coding agents (tools such as DSH load `AGENTS.md` / `CLAUDE.md` at the project root). Human readers should see `README.md`.

## Repository Nature

University course material archive (freshman `one/`, sophomore `two/`). It is **not a single buildable project**: there are no root-level build scripts, no CI, and no test framework. Each part is independent — when changing one part, only manage that part's toolchain; do not try to unify them.

For directory layout and contents of each part, see `README.md`; they are not repeated here.

## Verification That Must Be Run After Changes

| Change Target | Command (verified on this machine) |
| --- | --- |
| C++ single file | `g++ -std=gnu++17 -fsyntax-only <file>` |
| Web backend | `node --check two/javaweb/proj/server.js` |
| Static experiment page | Open `index.html` directly in a browser (no build step) |
| Java source | See Maven notes below |
| `three/Software_Construction` backend | `cd three/Software_Construction/backend` then `MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd -o test"` |
| `three/Software_Construction` frontend | `cd three/Software_Construction/frontend` then `npm run build` (see its own notes below) |
| Notes Markdown | No validation tool; just keep consistent with the existing file style |

Java build: `mvn` is **not on this machine's PATH**, and most of this repository has no `mvnw`. When compilation is needed, use the IDE's built-in Maven, or add Maven to PATH first, then run:

```bash
mvn -f two/Java/Projects/Java/pom.xml compile
```

Exception: `three/Software_Construction/backend` ships its own Maven wrapper (`mvnw` / `mvnw.cmd`), so it does not need a system Maven — see the notes for that project below.

### `three/Software_Construction` (Spring Boot 3.5 + Vue 3, the only buildable full-stack project here)

Commands and traps below were each hit for real on this machine, not copied from docs.

- **Running the wrapper from Git Bash needs `MSYS_NO_PATHCONV=1`.** Plain `sh ./mvnw ...` prints `sh: command not found` and can still leave the surrounding pipeline with exit code 0 — i.e. it will *look* like the build passed when nothing ran. Invoke it as `MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd -o <goal>"`.
- **Console and Maven/test log output is GBK, not UTF-8** (this is a Chinese Windows install), so Chinese in build output and in `logback` console logs arrives mangled. Pipe through `iconv -f GBK -t UTF-8` before reading it; do not conclude "the app lost the text" from a garbled terminal.
- **`SmokeTest` needs a running local MySQL80**; the other five test classes need no database. The `MySQL80` service is set to *Manual* start and an agent cannot start it (`Start-Service` fails with an access error), so a `SmokeTest` failure with `Unable to determine Dialect without JDBC metadata` means the database is down — **that is not a code regression**. Baseline is 54 tests.
- **`application.yml` and the frontend build are packaged *into* the jar.** Editing `backend/src/main/resources/application.yml` has no effect on a running jar, and neither does editing `frontend/src`. After a frontend change: `npm run build`, copy `frontend/dist/{assets,index.html}` into `backend/src/main/resources/static/`, then repackage. The key never belongs in the repo — `ai.api-key` stays as the placeholder `${AI_API_KEY:sk-请填写}` and the real value comes from the `AI_API_KEY` environment variable.
- **Repackaging fails with `Unable to rename ...jar.original` while the app is still running** (Windows holds the jar open). Stop the process listening on port 8088 first.
- **HTTP 200 does not mean success when probing this API by hand.** The `Result` envelope answers business failures as `200 + {"success":false,"message":...}`; read the body, not the status.
- **`@WebMvcTest`/MockMvc cannot prove everything about security here.** `SecurityMockMvcRequestPostProcessors.authentication()` sets `request.getUserPrincipal()`, which the real Tomcat request does not do at the point Spring's access-denied handler reads it — a defect that returned 401 on the live server passed as 403 under MockMvc. Anything about session/CSRF/status codes needs a real HTTP call to confirm.
- **There is no browser tool available here, and adding one (e.g. Playwright) would violate the "no new dependencies / no new build tools" rule below.** Vue component behavior in a real DOM cannot be verified by the agent; say so instead of reporting the UI as working.

## Pitfalls (Read Before Acting)

- **Note directory casing is inconsistent**: the git index records `Markdown/`, while the filesystem has `markdown/`; `core.ignorecase=true` masks the difference. When adding or referencing note files, use the git index `Markdown/` as the source of truth; do not do case-only renames, as they will create duplicate directories on case-sensitive systems.
- **There is no `.gitignore`**: `two/javaweb/proj/node_modules/`, Maven `target/`, and Quartus outputs (`db/`, `incremental_db/`, `*.rpt`, `*.sof`, `*.pof`, etc.) are already committed. Do not casually delete files that "look like build artifacts".
- In `.vscode/c_cpp_properties.json`, `compilerPath` points to the nonexistent `D:/AAA_Software/Code/mingw64/bin/g++.exe`; the actual g++ on this machine is at `D:/AAA_Software/Code/Environment/mingw64/bin/g++.exe` (11.2.0).
- **Quartus proprietary/binary formats** (`.bdf`, `.qpf`, `.vwf`, `.qws`, `.bsf`) must not be manually edited or generated from scratch; changes should be made in Quartus. The safely editable text files are `.vhd`, `.tdf`, `.mif`.
- Reports and documents are docx/pptx/pdf; do not rewrite them by text editing. Filenames include course, student ID, and name (e.g., `软件1244 陈伟生 202411701404.docx`); do not rename them.

## Conventions

- Use Chinese for documentation, comments, and commit messages, consistent with the current repository.
- Java exercises follow the existing `com.<topic>` package structure (`example` / `GUI` / `Server` / `Experiment` / `Runnable_demo` / `Test`); place new code in existing packages.
- `two/javaweb/proj/database.sql` is the single source of truth for database schema and initial data; when changing table schema, sync the queries and APIs in `server.js`.
- Do not introduce new dependencies, new build tools, or refactor the directory structure or unify code style for coursework; keep changes minimal.