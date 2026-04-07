# Backend 실행

## 가상환경으로 실행

리포지토리 루트 `venv`를 사용해서 Docker 없이 바로 실행할 수 있다.

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\run-backend.ps1
```

자동 리로드로 실행:

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\run-backend.ps1 -Reload
```

백그라운드 실행:

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\start-backend-bg.ps1
```

## 환경 변수

`backend\.env`가 있으면 먼저 읽는다.

예시:

```dotenv
SECRET_KEY=replace-with-random-secret
DATABASE_URL=sqlite:///./data/wordbook_server.db
```

`backend\.env`가 없으면 기본값은 아래처럼 적용된다.

- `SECRET_KEY=local-dev-secret-key`
- `DATABASE_URL=sqlite:///./data/wordbook_server.db`

## 확인

```powershell
Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:8000/health'
```
