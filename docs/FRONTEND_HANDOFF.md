# 프론트엔드 전달 사항

## 지금 연동 가능한 범위

- Google 로그인·로그아웃과 로그인 세션 조회
- 온보딩 완료 처리
- 그룹 생성·목록·상세·초대 코드 가입
- 일정 초안·조건·참여자 저장과 제출
- 참여자별 가능 날짜 제출 및 제출 완료 여부 조회

Calendar 실제 조회·등록, Kakao 장소 검색, AI 추천·재생성·확정은 아직 미구현입니다.

## 인증 규칙

1. 로그인 버튼은 백엔드 `GET /api/auth/google`로 페이지를 이동시킵니다.
2. 모든 API fetch에 `credentials: 'include'`를 지정합니다.
3. 앱 진입 시 `GET /api/auth/session`을 호출합니다.
4. 응답의 `authenticated=false`면 로그인 화면, `user.onboardingCompleted=false`면
   온보딩, 그 외에는 홈으로 이동합니다.
5. 상태 변경 요청 전에 `GET /api/auth/csrf`를 호출하고 반환된
   `headerName: token`을 헤더에 넣습니다.
6. `X-User-Id`는 더 이상 보내지 않습니다.

개발 환경에서 프론트 주소는 백엔드 `.env`의 `FRONTEND_ORIGIN`과 정확히 같아야
합니다. 로그인 성공 후 이동할 프론트 주소는 `OAUTH_LOGIN_SUCCESS_URI`로 정합니다.

## 세션 응답에서 사용할 값

`GET /api/auth/session`:

```json
{
  "authenticated": true,
  "user": {
    "id": 1,
    "googleSubject": "google-sub",
    "email": "user@example.com",
    "nickname": "사용자",
    "picture": "https://...",
    "onboardingCompleted": false
  },
  "calendarAuthorized": true,
  "grantedScopes": ["openid", "profile", "email"]
}
```

토큰은 프론트로 전달하지 않습니다. 브라우저 세션 쿠키로 인증합니다.

## 일정 관련 확정 규칙

- 그룹은 공유 초대 코드로 가입하며 로그인된 사용자만 가능합니다.
- 일정 참여자 지정에는 `userId`가 아니라 그룹 상세의 `memberId`를 보냅니다.
- 조건·참여자 수정, 일정 제출, 조율 시작 권한은 그룹 HOST가 아니라 일정 생성자에게 있습니다.
- 가능 날짜는 일정 참여자만 제출하며 1개 이상 선택합니다. 탐색 범위가 설정된 경우에는 그 범위 안의 날짜만 허용합니다.
- 전원이 제출한 뒤에는 수정할 수 없습니다.
- 다른 참여자의 상세 날짜는 공개하지 않고 제출 완료 여부만 표시합니다.
- 일정 상태는 `DRAFT → SURVEYING → READY_TO_PLAN → PLANNING` 순서입니다.

전체 요청·응답 예시는 [API_SPEC.md](API_SPEC.md), OAuth 실행 설정은
[google-oauth.md](google-oauth.md)를 참고합니다.
