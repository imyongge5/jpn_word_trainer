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

현재 프로젝트는 루트의 `version.properties`를 기준으로 `versionName`과 `versionCode`를 자동 계산한다.

### beta 푸시 규칙

- `beta`에 푸시할 때는 [push-beta.ps1](/E:/forfun/단어장앱/scripts/push-beta.ps1) 또는 [push-beta.bat](/E:/forfun/단어장앱/scripts/push-beta.bat)을 사용한다.
- 이 스크립트는 다음 작업을 자동으로 수행한다.
  - `VERSION_CCC`를 1 증가
  - `version.properties`를 커밋
  - `beta` 브랜치로 푸시
- 새 베타 라인을 시작할 때는 `VERSION_B`를 수동으로 올리고 `VERSION_CCC`를 `0` 또는 `1`로 초기화한다.
- 새 정식 세대를 시작할 때는 `VERSION_A`를 올리고 `VERSION_B`, `VERSION_CCC`를 초기화한다.

## 태그 운영 규칙

- 정식 릴리즈 태그
  - `v1.2.0`
- 베타 릴리즈 태그
  - `beta-v0.1.22`

태그 기반 자동화가 추가되면:

- `beta`의 태그는 베타 빌드/테스트 배포에 사용
- `main`의 태그는 정식 릴리즈 기준으로 사용

## 원칙

- `main`은 항상 안정판으로 유지한다.
- `beta`는 다음 릴리즈를 검증하는 공간으로 사용한다.
- 정식 배포 전에는 가능하면 `beta`에서 먼저 확인한다.
- 릴리즈 자동화가 붙기 전까지도 브랜치와 버전 규칙은 동일하게 유지한다.
