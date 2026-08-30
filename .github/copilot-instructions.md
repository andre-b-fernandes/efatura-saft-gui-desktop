# Copilot instructions for this repository

## Repository shape

- The real application lives in `gui-wrapper/`. The repository root mainly holds the upstream AT JAR, sample SAF-T XML files, and top-level docs.
- Treat `gui-wrapper` as the effective project root for source changes, builds, and packaging work.

## Build, run, and packaging commands

Run these from `gui-wrapper`:

```powershell
.\build.bat
.\run.bat
java -cp .\bin EFaturaGui
.\package-exe.bat
```

- `build.bat` compiles `src\*.java` directly into `bin\` with `javac`; there is no Maven, Gradle, or IDE project file.
- `run.bat` rebuilds first, then launches the Swing app.
- `package-exe.bat` rebuilds, creates `dist\EFaturaGuiWrapper.jar`, copies `..\FACTEMICLI-2.9.1-100067-cmdClient.jar` into `dist\` when present, and then runs `jpackage --type exe`.
- Packaging expects `jpackage` and WiX on `PATH`. If `jpackage` is missing, `package-exe.bat` first tries `load-env.cmd`, which loads values from the local `.env`.

## Tests and linting

- There is currently **no automated test suite** and **no linter** configured in this repository.
- There is also no single-test command because tests are not set up.
- The closest manual smoke check is:

```powershell
.\run.bat
```

Then exercise the GUI against a local AT JAR and a SAF-T XML file.

## Mandatory verification after edits

- After making ANY change to files under `gui-wrapper/src`, you MUST run `.\build.bat` from `gui-wrapper` and confirm it prints `Build concluido com sucesso.` before considering the change complete.
- If the build fails, fix the compile error and rebuild until it succeeds. Never report a source change as done without a passing build.

## High-level architecture

- The app is split into modular components under `src/` (see `gui-wrapper/DESIGN.md` for detailed design specifications):
  - `EFaturaGui.java`: Main Swing JFrame window, component layout, and user event wiring.
  - `AtClientRunner.java`: Command array construction, password masking, process lifecycle (`ProcessBuilder`), native charset stream reading, and automated self-update response.
  - `InputValidator.java`: Pre-flight input validation rules for NIF, password, year, month, and file paths.
  - `ValidationResult.java`: Immutable result value object (ok flag and message).
  - `JarLocator.java`: File discovery for upstream `FACTEMICLI` client JARs, SAF-T XML files, and date defaults.
  - `UiScaler.java`: Dynamic font scaling and component sizing (95%, 100%, 110%) with base font caching.
- The GUI is a thin wrapper over the official AT command-line client. The app does **not** implement SAF-T submission logic itself; instead it collects inputs and launches `java -jar <AT client> ...`.
- Runtime flow:
  1. Startup: `EFaturaGui` initializes UI layout and uses `JarLocator` to seed default file paths and current date.
  2. Input validation: `InputValidator.validate()` checks inputs before starting process execution.
  3. Process launch: `AtClientRunner.buildCommand()` translates form fields into AT CLI flags (`-n`, `-p`, `-a`, `-m`, `-op`, `-i`, optional `-t`). `AtClientRunner.runProcess()` executes the process inside a `SwingWorker`.
  4. Stream logging & update handling: `AtClientRunner` streams live `stdout`/`stderr` logs and automatically answers interactive AT client self-update prompts on standard input.
  5. Process termination: `AtClientRunner.stopProcess()` sends `Process.destroy()` to the running AT client.
- The GitHub Actions release workflow (`.github\workflows\release-master.yml`) is Windows-only. On pushes to `master`, it installs Java 21 and WiX, runs `gui-wrapper\package-exe.bat`, and publishes the generated installer from `gui-wrapper\release\`.

## Key conventions in this codebase

- This is a **Windows-first** repository. Scripts, packaging, and release automation all assume Windows batch/PowerShell tooling and `jpackage --type exe`.
- Keep changes aligned with the wrapper's current scope. The UI intentionally exposes only a subset of the upstream AT client flags; README documents `-o`, `-md`, `-af`, and `-ea` as intentionally omitted.
- When changing process execution, preserve the current `ProcessBuilder` argument-list approach instead of building a single shell command string. That avoids quoting issues for paths with spaces.
- Never log the raw password. The displayed command preview must continue to use the masking behavior in `maskPasswordForDisplay()`.
- The UI must remain responsive while the AT client runs. Long-running execution belongs in the existing `SwingWorker` + background log-thread pattern, not on the EDT.
- Default file discovery is relative to the current working directory and assumes the upstream JAR and sample XML may exist one level above `gui-wrapper`. Be careful not to break that local workflow when moving files or changing launch scripts.
