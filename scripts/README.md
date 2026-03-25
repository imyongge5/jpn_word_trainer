# Scripts

- Build test: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1`
- Build test now also generates the prebuilt seed database at `app/src/main/assets/databases/wordbook.db` before Gradle runs.
- Custom Gradle task: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -GradleTask test`
- Skip emulator install: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -SkipInstall`
- Prefer emulator even when a real device is connected: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -PreferEmulator`
- Enrich bundled words from JMdict examples: `.\venv\Scripts\python.exe .\scripts\enrich_jmdict.py --download-if-missing`
- Generate prebuilt seed database only: `.\venv\Scripts\python.exe .\scripts\generate_seed_db.py`
