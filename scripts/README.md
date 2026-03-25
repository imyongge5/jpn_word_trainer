# Scripts

- Build test: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1`
- Custom Gradle task: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -GradleTask test`
- Skip emulator install: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -SkipInstall`
- Skip opening Figma: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -SkipFigma`
- Enrich bundled words from JMdict examples: `.\venv\Scripts\python.exe .\scripts\enrich_jmdict.py --download-if-missing`
