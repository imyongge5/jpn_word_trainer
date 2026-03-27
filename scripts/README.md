# Scripts

- Build test: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1`
- Build test now also generates the prebuilt seed database at `app/src/main/assets/databases/wordbook.db` before Gradle runs.
- Seed source JSON is now build-only and lives at `data/seeds/jlpt_words.json`, so it is not packaged into the APK.
- Custom Gradle task: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -GradleTask test`
- Skip emulator install: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -SkipInstall`
- Prefer emulator even when a real device is connected: `powershell -ExecutionPolicy Bypass -File .\scripts\build-test.ps1 -PreferEmulator`
- CI/build scripts are split into Python steps under `scripts/ci_*.py`.
- CI script dependencies are documented in `scripts/requirements-ci.txt`.
- Build tag workflow now runs the Python CI scripts sequentially for version calculation, Firebase prep, release build, GitHub release publish, and Firebase distribution.
- User-facing patch notes only include commits whose subject starts with one of: `added_feat:`, `bug_fix:`, `design_fix:`, `changed:`.
- Prefix가 없는 작업 커밋은 패치노트에 포함되지 않으므로, 실제 사용자에게 보여줄 변경이 완성됐을 때만 위 prefix를 붙입니다.
- Copy `scripts/local-ci.env.example` to `scripts/local-ci.env` and fill in the local values you want to test with.
- Local CI workflow smoke test:
  `powershell -ExecutionPolicy Bypass -File .\scripts\test-ci-workflow.ps1`
- Skip remote release publish while testing locally:
  `powershell -ExecutionPolicy Bypass -File .\scripts\test-ci-workflow.ps1 -SkipPublishRelease`
- Test manual Firebase redistribute flow locally:
  `powershell -ExecutionPolicy Bypass -File .\scripts\test-ci-workflow.ps1 -WorkflowMode DistributeFirebase -ReleaseTag v0.1.26`
- To use a different env file path, pass `-EnvFilePath .\path\to\local-ci.env`.
- If you want a one-off local bootstrap for the service account secret only, pass `-ServiceAccountPath .\your-service-account.json`.
- Enrich bundled words from JMdict examples: `.\venv\Scripts\python.exe .\scripts\enrich_jmdict.py --download-if-missing`
- Generate prebuilt seed database only: `.\venv\Scripts\python.exe .\scripts\generate_seed_db.py`
