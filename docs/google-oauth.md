# Google OAuth 연동

Damoyeo 백엔드는 Spring Security OAuth2 Client를 사용해 Google 로그인을 처리합니다. OAuth 설정은 저장소 루트의 `.env`에서 읽으며 Google Access Token이나 Refresh Token은 API 응답으로 노출하지 않습니다.

## 필요한 `.env` 변수

```dotenv
GOOGLE_CLIENT_ID=발급받은-client-id
GOOGLE_CLIENT_SECRET=발급받은-client-secret
GOOGLE_REDIRECT_URI=Google-Cloud에-등록한-전체-리디렉션-URI
```

선택 설정은 다음과 같습니다.

```dotenv
OAUTH_LOGIN_SUCCESS_URI=/api/auth/session
SESSION_COOKIE_SECURE=false
```

운영 HTTPS 환경에서는 `SESSION_COOKIE_SECURE=true`를 사용합니다. `.env`는 Git에서 제외되며 실제 비밀값을 커밋하지 않습니다.

## Google Cloud 설정

1. Google Cloud 프로젝트에서 Google Calendar API를 활성화합니다.
2. OAuth 동의 화면에 로그인할 계정을 테스트 사용자로 등록합니다.
3. OAuth 2.0 웹 클라이언트의 승인된 리디렉션 URI에 `.env`의 `GOOGLE_REDIRECT_URI`와 정확히 같은 값을 등록합니다.

애플리케이션은 Redirect URI의 callback 경로를 Spring Security에 자동으로 적용합니다. 로컬 URI의 포트가 기본 8080과 다르면 아래 전용 작업이 같은 포트로 서버를 실행합니다.

```bash
./gradlew oauthBootRun
```

PostgreSQL은 기존 프로젝트 실행 방법대로 먼저 준비해야 합니다.

## 인증 API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/auth/google` | Google 로그인 시작 |
| `GET` | `/api/auth/session` | 현재 로그인 사용자와 Calendar 권한 상태 조회 |
| `GET` | `/api/auth/csrf` | 상태 변경 요청에 사용할 CSRF 토큰 조회 |
| `POST` | `/api/auth/logout` | 세션 무효화 및 로그아웃 |

로그인 성공 후 기본 이동 경로는 `/api/auth/session`입니다. 프런트엔드 경로가 필요하면 `OAUTH_LOGIN_SUCCESS_URI`로 변경합니다.

로그인이 완료되면 Google `sub`를 기준으로 `users` 테이블에 사용자를 생성하거나 이메일·닉네임을 갱신합니다. 이후 그룹과 미팅 API는 세션의 Google 사용자와 DB 사용자를 연결하므로 `X-User-Id` 헤더를 사용하지 않습니다.

`GET /api/auth/session`의 `user` 응답에는 내부 사용자 `id`, `googleSubject`, `email`, `nickname`, `picture`, `onboardingCompleted`가 포함됩니다. Access Token과 Refresh Token은 응답하지 않습니다.

## 프런트엔드 호출 예시

세션 쿠키를 사용하므로 API 요청에 credentials를 포함합니다.

```javascript
const session = await fetch('/api/auth/session', {
  credentials: 'include',
}).then((response) => response.json());
```

로그아웃 전 CSRF 토큰을 조회해 응답의 `headerName`과 `token`을 그대로 전달합니다.

```javascript
const csrf = await fetch('/api/auth/csrf', {
  credentials: 'include',
}).then((response) => response.json());

await fetch('/api/auth/logout', {
  method: 'POST',
  credentials: 'include',
  headers: {
    [csrf.headerName]: csrf.token,
  },
});
```

Google 로그인 시 `openid`, `profile`, `email`과 Calendar 일정 및 Free/Busy scope를 함께 요청합니다. `/api/auth/session`의 `calendarAuthorized`로 두 Calendar scope가 모두 승인되었는지 확인할 수 있습니다.
