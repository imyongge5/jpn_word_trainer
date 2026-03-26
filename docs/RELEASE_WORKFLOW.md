# 브랜치와 릴리즈 운영 규칙

## 브랜치 역할

- `main`
  - 정식 배포 기준 브랜치
  - 항상 배포 가능한 안정 상태를 유지한다
  - 직접 푸시하지 않고 `Pull Request`로만 반영한다
- `beta`
  - 다음 정식 버전 후보를 검증하는 브랜치
  - 새 기능과 수정사항을 먼저 모아 테스트한다
  - 충분히 안정화되면 `beta -> main`으로 승격한다

## 작업 흐름

1. 새 작업은 별도 작업 브랜치에서 시작한다.
2. 작업이 끝나면 먼저 `beta`로 `Pull Request`를 보낸다.
3. 베타 테스트와 검증을 `beta`에서 진행한다.
4. 정식 배포가 가능하다고 판단되면 `beta -> main`으로 `Pull Request`를 보낸다.
5. `main`에 머지된 상태를 정식 버전으로 본다.

## 머지 규칙

- `main`
  - 직접 푸시 금지
  - `Pull Request`로만 반영
  - force push 금지
- `beta`
  - 현재는 보호 규칙 없음
  - 필요 시 나중에 PR 중심 규칙으로 강화 가능

## 버전 규칙

- 버전 표기 형식
  - `vA.B.CCC`
- 의미
  - `A`: 정식 세대 번호
  - `B`: 베타 라인 번호
  - `CCC`: 같은 베타 라인에서 푸시할 때마다 1씩 증가하는 번호
- 예시
  - `v0.1.22`
  - `v1.3.4`

같은 기능 묶음은 먼저 베타 버전으로 검증하고, 검증 완료 후 정식 버전으로 승격한다.

### Android 버전 매핑

- `versionName`
  - `vA.B.CCC` 그대로 사용
- `versionCode`
  - `A * 100000 + B * 1000 + CCC`

현재 프로젝트는 루트의 `version-series.properties`와 Git 히스토리를 기준으로 `versionName`과 `versionCode`를 자동 계산한다.

### beta 운영 규칙

- 개발자는 `CCC`를 직접 관리하지 않는다.
- `version-series.properties`에는 `A`, `B`만 저장한다.
- 새 베타 라인을 시작할 때는 `VERSION_B`를 수동으로 올린다.
- 새 정식 세대를 시작할 때는 `VERSION_A`를 올리고 `VERSION_B`를 원하는 시작값으로 정리한다.

### 빌드 태그 규칙

- 빌드 트리거 태그
  - `build`

`build` 태그는 별도 빌드용 움직이는 태그다. `beta` 버전 계산만으로는 바로 빌드하지 않는다.

빌드 워크플로우는 `build` 태그에서:

- 해당 커밋의 Git 히스토리에서 현재 `vA.B.CCC`를 계산해
- 앱의 `versionName`, `versionCode`에 반영하고
- 릴리즈 APK를 빌드하고
- 빌드가 성공하면 `vA.B.CCC` 버전 태그를 생성하고
- Firebase App Distribution 배포와 GitHub Release 생성을 수행한다.

`build` 태그는 필요할 때마다 다른 커밋으로 옮겨 붙여 재사용한다.

예시:

```bash
git tag -fa build <커밋해시>
git push origin -f build
```

## 태그 운영 규칙

- 정식 릴리즈 태그
  - `v1.2.0`
- 빌드 트리거 태그
  - `build`

태그 기반 자동화가 추가되면:

- `build` 태그는 선택적 릴리즈 APK 빌드 실행에 사용
- 성공한 `build` 실행은 `vA.B.CCC` 버전 태그, GitHub Release, Firebase 배포를 남긴다
- `main`의 태그는 정식 릴리즈 기준으로 사용

### GitHub Secret

- `FIREBASE_SERVICE_ACCOUNT`
  - Firebase App Distribution 업로드용 서비스 계정 JSON 전체 내용
- `FIREBASE_APP_DISTRIBUTION_GROUPS` 또는 `FIREBASE_APP_DISTRIBUTION_TESTERS`
  - Firebase 배포 대상
- `WORKFLOW_PUSH_TOKEN`
  - GitHub Actions가 `vA.B.CCC` 버전 태그를 푸시할 때 사용하는 토큰
  - workflow 파일이 포함된 커밋에도 태그를 만들 수 있도록 `Contents: write` 와 `Workflows: write` 권한이 필요하다

## 워크플로 히스토리

- GitHub Actions의 **실행 기록(run history)** 은 계속 쌓인다.
- 하지만 워크플로 정의 파일 자체는 최신 커밋의 내용만 사용한다.
- 즉, 예전 실행 결과는 남아도 워크플로 YAML이 누적 수정되는 구조는 아니다.

## 원칙

- `main`은 항상 안정판으로 유지한다.
- `beta`는 다음 릴리즈를 검증하는 공간으로 사용한다.
- 정식 배포 전에는 가능하면 `beta`에서 먼저 확인한다.
- 릴리즈 자동화가 붙기 전까지도 브랜치와 버전 규칙은 동일하게 유지한다.
