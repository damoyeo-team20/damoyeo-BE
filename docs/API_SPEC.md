# Damoyeo API 명세

## 공통

```text
Base URL: http://localhost:8080/api
Content-Type: application/json
```

Google OAuth 로그인 후 발급되는 세션 쿠키를 사용합니다. 프론트 요청에는
`credentials: 'include'`가 필요하며 `X-User-Id` 헤더는 사용하지 않습니다.

## 인증

```http
GET  /api/auth/google
GET  /api/auth/session
GET  /api/auth/csrf
POST /api/auth/logout
```

- 로그인 시작은 링크 이동 또는 `window.location.href`로 `/api/auth/google`을 엽니다.
- `GET /api/auth/session`의 `authenticated`, `user.onboardingCompleted`,
  `calendarAuthorized`로 초기 화면을 결정합니다.
- `POST`, `PUT`, `PATCH`, `DELETE` 요청 전 `/api/auth/csrf`에서 받은
  `headerName`과 `token`을 요청 헤더에 넣습니다.
- 인증되지 않은 보호 API는 `401`을 반환합니다.

## 사용자

```http
GET  /api/users/me
POST /api/users/me/onboarding/complete
```

`onboarding/complete`는 온보딩 완료와 건너뛰기 모두에서 호출합니다.

## 그룹

### 그룹 생성

```http
POST /api/groups
```

```json
{ "name": "대학교 동기" }
```

Response `201`:

```json
{
  "id": 1,
  "name": "대학교 동기",
  "inviteCode": "7KPX9MQR",
  "members": [
    { "memberId": 1, "userId": 1, "role": "HOST" }
  ],
  "createdAt": "2026-08-19T06:00:00Z"
}
```

### 내 그룹 목록

```http
GET /api/groups
```

```json
[
  { "id": 1, "name": "대학교 동기", "createdAt": "2026-08-19T06:00:00Z" }
]
```

### 그룹 상세

```http
GET /api/groups/{groupId}
```

Response는 그룹 생성 응답과 같습니다.

멤버 응답에는 `nickname`, `role`, `preferenceCount`가 포함됩니다.

### 초대 코드로 그룹 가입

```http
POST /api/groups/join
```

```json
{ "inviteCode": "7KPX9MQR" }
```

## 일정

### 일정 초안 생성

```http
POST /api/groups/{groupId}/meetings
```

Request Body 없음. `DRAFT` 상태의 일정을 반환합니다.

### 일정 조건 저장

```http
PUT /api/meetings/{meetingId}/conditions
```

```json
{
  "purpose": "오랜만에 저녁 식사",
  "region": "건대",
  "scheduleSearchFrom": "2026-08-23",
  "scheduleSearchTo": "2026-09-07",
  "preferredTimeOfDay": "EVENING",
  "preferenceSurveyDeadline": "2026-08-22"
}
```

`preferredTimeOfDay`:

```text
DAYTIME | LATE_AFTERNOON | EVENING | ANY
```

### 일정 참여자 저장

```http
PUT /api/meetings/{meetingId}/participants
```

```json
{ "groupMemberIds": [1, 2, 3] }
```

그룹 상세 응답의 `members[].memberId`를 전달합니다.

### 일정 조회

```http
GET /api/meetings/{meetingId}
```

Response `200`:

```json
{
  "id": 1,
  "groupId": 1,
  "createdBy": 1,
  "purpose": "오랜만에 저녁 식사",
  "region": "건대",
  "scheduleSearchFrom": "2026-08-23",
  "scheduleSearchTo": "2026-09-07",
  "preferredTimeOfDay": "EVENING",
  "preferenceSurveyDeadline": "2026-08-22",
  "status": "SURVEYING",
  "participantMemberIds": [1, 2, 3],
  "createdAt": "2026-08-19T06:00:00Z",
  "updatedAt": "2026-08-19T06:10:00Z"
}
```

### 일정 작성 완료

```http
POST /api/meetings/{meetingId}/submit
```

Request Body 없음.

- 제출 직후: `COLLECTING_AVAILABILITY`
- 모든 참여자의 가능 날짜 제출 후 조사 마감 전: `SURVEYING`
- 모든 참여자의 가능 날짜 제출 후 조율 가능: `READY_TO_PLAN`

### 내 가능 날짜 제출

```http
PUT /api/meetings/{meetingId}/availability
```

```json
{ "availableDates": ["2026-08-23", "2026-08-24"] }
```

```http
GET /api/meetings/{meetingId}/availability/me
GET /api/meetings/{meetingId}/coordination
```

### AI 조율 시작

```http
POST /api/meetings/{meetingId}/plan
```

Request Body 없음. 상태를 `PLANNING`으로 변경합니다. 실제 AI 호출은 아직 구현되지 않았습니다.

## 권한

- 그룹 멤버는 그룹과 일정을 조회할 수 있습니다.
- 일반 멤버도 일정을 만들 수 있습니다.
- 조건 수정, 참여자 수정, 제출, AI 조율 시작은 일정 생성자만 가능합니다.
- 그룹 HOST도 자신이 만들지 않은 일정은 수정할 수 없습니다.

## 일정 상태

```text
DRAFT | COLLECTING_AVAILABILITY | SURVEYING | READY_TO_PLAN | PLANNING
PROPOSING | CONFIRMED | FAILED | CANCELLED
```

## 에러 형식

```json
{
  "code": "MEETING_EDIT_FORBIDDEN",
  "message": "일정을 만든 사용자만 수정할 수 있습니다.",
  "fieldErrors": [],
  "timestamp": "2026-08-19T06:00:00Z"
}
```

## 미구현

- Google Calendar 조회·일정 등록
- Kakao 장소 검색
- 실제 AI 채팅 및 조율
- 장소 제안·재생성·확정
