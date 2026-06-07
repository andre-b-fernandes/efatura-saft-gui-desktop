# e-Fatura JAR GUI Wrapper

Desktop app in Java Swing that wraps the official AT e-Fatura JAR so you can run it from a GUI.

## Current scope

- Supported parameters: `-n`, `-p`, `-a`, `-m`, `-op`, `-i`, `-t`
- Intentionally omitted: `-o`, `-md`, `-af`, `-ea`
- Real-time logs of `stdout` and `stderr` in the UI
- No XML response parsing yet (raw logs only)

## Project structure

- `src/EFaturaGui.java`: main GUI and process execution logic
- `build.bat`: compiles Java source to `bin`
- `run.bat`: runs build, then launches the GUI
- `package-exe.bat`: builds a Windows `.exe` installer using `jpackage`
- `bin/`: compiled `.class` files

## Requirements

- Java installed and available in `PATH`
	- `java`
	- `javac`
- Official AT JAR available locally
	- Default expected location: workspace root
	- Example: `..\FACTEMICLI-2.9.1-100067-cmdClient.jar` (auto-filled at startup if present)

## Development setup

1. Open a terminal in this folder:

```powershell
cd c:\Users\ferna\Documents\EFatura\gui-wrapper
```

2. Confirm Java tools:

```powershell
java -version
javac -version
```

3. Build the app:

```powershell
.\build.bat
```

If compilation succeeds, classes are written to `bin`.

## Load environment variables on Windows

This project includes a local `.env` file with Java packaging settings.

PowerShell (current session):

```powershell
.\load-env.ps1
```

PowerShell (persist for future terminals, user scope):

```powershell
.\load-env.ps1 -PersistUser
```

CMD (current session):

```cmd
call load-env.cmd
```

Notes:

- The values are loaded into the current terminal session only.
- Open a new terminal and run the loader again if needed.
- If you used `-PersistUser`, open a new terminal to pick up user-level changes.
- After loading, you can verify with `jpackage --version`.

## Launch the app

Recommended (build + run):

```powershell
.\run.bat
```

Alternative (run only, if already built):

```powershell
java -cp .\bin EFaturaGui
```

## Generate a Windows .exe

This project includes an automated script for packaging.

Prerequisites:

- A full JDK with `jpackage` available in `PATH`
- In many setups, this means installing a modern JDK and ensuring `...\\bin` is on `PATH`
- Validate with:

```powershell
jpackage --version
```

Generate the installer:

```powershell
.\package-exe.bat
```

What the script does:

1. Compiles the project (`build.bat`)
2. Creates `dist\EFaturaGuiWrapper.jar` from compiled classes
3. Copies `..\FACTEMICLI-2.9.1-100067-cmdClient.jar` to `dist` if found
4. Runs `jpackage --type exe`
5. Writes the generated installer to `release`

Distribution notes:

- The generated `.exe` installer is Windows-only.
- On target machines, users do not need to run from source code.
- Keep your official AT JAR policy in mind; replace/update the bundled JAR as needed.

## How to use the GUI

1. Confirm or select the JAR path.
2. Fill `NIF`, `Password`, `Ano (YYYY)`, and `Mes (MM)`.
3. Choose operation: `validar` or `enviar`.
4. Select the input SAF-T XML file.
5. Optionally enable test mode (`-t`).
6. Click `Executar`.
7. Read live logs in the lower panel.

## Developer notes

- The app builds command arguments as a list (`ProcessBuilder`) to avoid quoting issues with spaces in file paths.
- Password is masked in the displayed command preview in logs.
- The process is run asynchronously so the UI stays responsive.
- `Parar` sends `destroy()` to the running process.

## Troubleshooting

- `java` or `javac` not found:
	- Install JDK and ensure Java binaries are in `PATH`.
- JAR path invalid:
	- Select the correct file using the `...` button.
- Input XML invalid or missing:
	- Confirm the selected file exists and is readable.
- Authentication / server errors:
	- Check NIF/password, internet connection, and machine clock synchronization.
- Build fails:
	- Re-run `javac -version` and verify you have a full JDK (not only JRE).
- `jpackage` not found:
	- Install a JDK that includes `jpackage` and ensure its `bin` folder is in `PATH`.

## Next planned improvements

- Profile save/load for frequent parameter sets
- Optional parsing of XML responses into friendly status cards
- Better handling of interactive prompts from the AT client
