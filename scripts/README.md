# Scripts

- Build test: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1`
- Custom Gradle task: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -GradleTask test`
- Skip emulator install: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -SkipInstall`
- Skip opening Figma: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -SkipFigma`
- Skip syncing exported design tokens: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -SkipDesignSync`
- Sync exported design tokens only: `powershell -ExecutionPolicy Bypass -File .\scripts\sync-figma-tokens.ps1`
- Enrich bundled words from JMdict examples: `.\venv\Scripts\python.exe .\scripts\enrich_jmdict.py --download-if-missing`

## Figma design token sync

- Export Figma color tokens into [design/figma-tokens.json](<WORKSPACE_PATH>\design\figma-tokens.json)
- The build script automatically applies those tokens to [Color.kt](<WORKSPACE_PATH>\app\src\main\java\com\example\wordbookapp\ui\theme\Color.kt) before building
- This is a local token sync pipeline, not a direct live Figma API pull
