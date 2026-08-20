# Damoyeo API 명세서

이 문서는 현재 코드에서 실제 호출 가능한 API를 기준으로 작성한다.
알림 전송은 아직 구현되지 않았으며 AI·캘린더 연동은 외부 서비스 설정이 필요하다.

## 1. 공통

```text
Base URL: http://localhost:8080/api
Content-Type: application/json
```

Google OAuth 로그인 후 발급되는 서버 세션 쿠키(`JSESSIONID`)를 사용한다.

- 모든 요청에 `credentials: 'include'`를 지정한다.
- `X-User-Id` 헤더는 사용하지 않는다.
- `POST`, `PUT`, `PATCH`, `DELETE` 요청에는 CSRF 토큰을 전달한다.
- 인증되지 않은 보호 API는 `401 Unauthorized`를 반환한다.

```javascript
const csrf = await fetch(`${API_URL}/api/auth/csrf`, {
  credentials: 'include',
}).then(response => response.json());

await fetch(`${API_URL}/api/groups`, {
  method: 'POST',
  credentials: 'include',
  headers: {
    'Content-Type': 'application/json',
    [csrf.headerName]: csrf.token,
  },
  body: JSON.stringify({ name: '대학교 동기' }),
});
```

### 에러 형식

```json
{
  "code": "MEETING_EDIT_FORBIDDEN",
  "message": "일정을 만든 사용자만 수정할 수 있습니다.",
  "fieldErrors": [],
  "timestamp": "2026-08-19T06:00:00Z"
}
```

### Enum

```text
MeetingStatus:
DRAFT | SURVEYING | READY_TO_PLAN | PLANNING
| PROPOSING | CONFIRMED | FAILED | CANCELLED

GroupMemberRole:
HOST | MEMBER

PreferredTimeOfDay:
DAYTIME | LATE_AFTERNOON | EVENING | ANY

PreferenceSentiment:
POSITIVE | NEGATIVE

PreferenceStrength:
WEAK | MODERATE | STRONG
```

`group_members.status`는 존재하지 않는다. `group_members` 행이 존재하면 가입된 멤버다.

## 2. 인증

### Google 로그인 시작

```http
GET /api/auth/google
```

- Request Body 없음
- Google 로그인 페이지로 `302` 리다이렉트
- 프론트는 fetch가 아니라 페이지 이동으로 호출한다.

```javascript
window.location.href = `${API_URL}/api/auth/google`;
```

로그인 성공 시 Google `sub`로 사용자를 조회한다. 최초 로그인은 `users` 행을 만들고,
기존 사용자는 이메일과 닉네임을 갱신한다. 토큰은 프론트에 반환하지 않는다.

### 로그인 세션 조회

```http
GET /api/auth/session
```

인증된 경우:

```json
{
  "authenticated": true,
  "user": {
    "id": 1,
    "googleSubject": "109876543210",
    "email": "abc@gmail.com",
    "nickname": "홍길동",
    "picture": "https://example.com/profile.png",
    "onboardingCompleted": false
  },
  "calendarAuthorized": true,
  "grantedScopes": ["openid", "profile", "email"]
}
```

인증되지 않은 경우에도 `200`이다.

```json
{
  "authenticated": false,
  "user": null,
  "calendarAuthorized": false,
  "grantedScopes": []
}
```

### CSRF 토큰 조회

```http
GET /api/auth/csrf
```

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "발급된-CSRF-토큰"
}
```

### 로그아웃

```http
POST /api/auth/logout
```

- CSRF 필요
- Response `204 No Content`

## 3. 사용자와 선호

### 내 정보 조회

```http
GET /api/users/me
```

```json
{
  "id": 1,
  "email": "abc@gmail.com",
  "nickname": "홍길동",
  "onboardingCompleted": false,
  "createdAt": "2026-08-19T06:00:00Z",
  "updatedAt": "2026-08-19T06:00:00Z"
}
```

### 온보딩 완료 또는 건너뛰기

```http
POST /api/users/me/onboarding/complete
```

Request Body 없음. 완료와 건너뛰기 모두 호출한다. 응답은 `GET /users/me`와 같다.

### 내 선호 목록

```http
GET /api/users/me/preferences
```

```json
[
  {
    "id": 31,
    "vocabularyCode": "SPICY_FOOD",
    "displayName": "매운 음식",
    "domain": "FOOD",
    "rawValue": "매운 음식",
    "sentiment": "POSITIVE",
    "strength": "MODERATE",
    "mappingType": "EXACT"
  }
]
```

미분류 선호는 `vocabularyCode`, `displayName`, `domain`이 `null`이고
`mappingType`이 `UNMAPPED`인 형태로 함께 반환한다.

### 내 선호 삭제

```http
DELETE /api/users/me/preferences/{preferenceId}
```

본인의 선호만 삭제할 수 있다. 성공 시 `204 No Content`다.

## 4. 그룹

### 내 그룹 목록

```http
GET /api/groups
```

그룹 생성일 내림차순으로 반환한다.

```json
[
  {
    "id": 1,
    "name": "대학교 동기",
    "memberCount": 2,
    "members": [
      { "userId": 1, "nickname": "홍길동", "role": "HOST" },
      { "userId": 2, "nickname": "김철수", "role": "MEMBER" }
    ],
    "lastMeeting": {
      "confirmedStartAt": "2026-07-12T18:00:00Z",
      "region": "건대"
    },
    "activeMeetings": [
      {
        "id": 20,
        "status": "SURVEYING",
        "region": "건대",
        "scheduleSearchFrom": "2026-08-23",
        "scheduleSearchTo": "2026-09-07",
        "createdBy": 1
      }
    ],
    "activeMeeting": { "id": 20, "status": "SURVEYING" },
    "createdAt": "2026-08-19T06:00:00Z"
  }
]
```

지난 확정 일정이 없으면 `lastMeeting`은 `null`이다. 진행 중인 일정은
`activeMeetings`에 최신순으로 모두 반환하며, `activeMeeting`은 기존 프론트 호환용 첫 항목이다.

### 그룹 삭제

```http
DELETE /api/groups/{groupId}
```

그룹 `HOST`만 호출할 수 있으며 성공 시 `204 No Content`다. 그룹의 일정과 관련 데이터도
함께 삭제된다.

### 그룹 생성

```http
POST /api/groups
```

```json
{ "name": "대학교 동기" }
```

- 이름 필수, 최대 100자
- 생성자는 즉시 `HOST`
- Response `201 Created`

```json
{
  "id": 1,
  "name": "대학교 동기",
  "inviteCode": "7KPX9MQR",
  "memberCount": 1,
  "pastMeetingCount": 0,
  "members": [
    {
      "memberId": 1,
      "userId": 1,
      "nickname": "홍길동",
      "role": "HOST",
      "preferenceCount": 0,
      "calendarConnected": false
    }
  ],
  "activeMeeting": null,
  "createdAt": "2026-08-19T06:00:00Z"
}
```

### 초대 코드 가입

```http
POST /api/groups/join
```

```json
{ "inviteCode": "7KPX9MQR" }
```

초대 코드는 영문·숫자 8자리이며 소문자로 보내도 처리한다.

```json
{
  "groupId": 1,
  "groupName": "대학교 동기",
  "groupMemberId": 5,
  "role": "MEMBER",
  "alreadyMember": false
}
```

이미 가입한 사용자와 그룹 생성자가 자신의 코드를 입력한 경우에도 `200`이며
`alreadyMember`가 `true`다. 잘못된 코드는 `404 INVITE_CODE_NOT_FOUND`다.

### 그룹 상세

```http
GET /api/groups/{groupId}
```

```json
{
  "id": 1,
  "name": "대학교 동기",
  "inviteCode": "7KPX9MQR",
  "memberCount": 2,
  "pastMeetingCount": 3,
  "members": [
    {
      "memberId": 1,
      "userId": 1,
      "nickname": "홍길동",
      "role": "HOST",
      "preferenceCount": 2,
      "calendarConnected": false
    }
  ],
  "activeMeetings": [
    {
      "id": 20,
      "status": "SURVEYING",
      "purpose": null,
      "region": "건대",
      "scheduleSearchFrom": "2026-08-23",
      "scheduleSearchTo": "2026-09-07",
      "createdBy": 1,
      "createdAt": "2026-08-20T08:00:00Z"
    }
  ],
  "activeMeeting": { "id": 20, "status": "SURVEYING" },
  "createdAt": "2026-08-19T06:00:00Z"
}
```

- 그룹 멤버만 조회할 수 있다.
- 진행 일정이 없으면 `activeMeetings`는 `[]`, `activeMeeting`은 `null`이다.
- 진행 일정이 여러 개면 `activeMeetings`에 최신순으로 모두 반환한다.
- `calendarConnected`는 아직 연동 상태를 저장하지 않아 항상 `false`다.
- 참여자 저장 시 `members[].memberId`를 사용한다.

### 그룹 일정 조회

```http
GET /api/groups/{groupId}/meetings?timing=UPCOMING
GET /api/groups/{groupId}/meetings?timing=UPCOMING_ALL
GET /api/groups/{groupId}/meetings?timing=PAST
```

`UPCOMING`은 가장 가까운 미래 확정 일정 하나를 반환한다.
`UPCOMING_ALL`은 미래 확정 일정을 가까운 순서의 배열로 반환한다. 여러 일정을 표시하는 그룹 상세 화면에서 사용한다.

```json
{
  "id": 12,
  "purpose": "오랜만에 저녁 식사",
  "region": "건대",
  "confirmedStartAt": "2026-08-30T19:00:00Z",
  "confirmedEndAt": "2026-08-30T21:00:00Z",
  "status": "CONFIRMED"
}
```

일정이 없으면 현재 응답은 `200` 빈 본문이다. 프론트는 `Content-Length: 0`을
처리해야 하며 무조건 `response.json()`을 호출하면 안 된다.

`PAST`는 최근 확정 일시 순 배열이다.

```json
[
  {
    "id": 8,
    "purpose": "여름 모임",
    "region": "홍대",
    "confirmedStartAt": "2026-07-12T18:00:00Z",
    "confirmedEndAt": "2026-07-12T20:00:00Z",
    "status": "CONFIRMED"
  }
]
```

`timing`을 생략하거나 다른 값을 보내면 모든 상태의 일정을 배열로 반환한다.

## 5. 일정

일정 생성자 ID는 `createdBy`다. 그룹 `HOST` 여부와 관계없이 일정 생성자만 조건,
참여자, 제출 및 조율 상태를 변경할 수 있다.

### 일정 초안 생성

```http
POST /api/groups/{groupId}/meetings
```

Request Body 없음. 그룹의 일반 멤버도 생성할 수 있다. Response `201 Created`이며
아래 일정 상세 형식에서 값이 없는 조건은 `null`, 참여자 배열은 빈 배열이다.

### 일정 상세

```http
GET /api/meetings/{meetingId}
```

```json
{
  "id": 20,
  "groupId": 1,
  "createdBy": 1,
  "purpose": "오랜만에 저녁 식사",
  "region": "건대",
  "scheduleSearchFrom": "2026-08-23",
  "scheduleSearchTo": "2026-09-07",
  "preferredTimeOfDay": "EVENING",
  "resolvedStartAt": null,
  "resolvedEndAt": null,
  "scheduleResolutionReason": null,
  "confirmedSuggestion": null,
  "status": "DRAFT",
  "participantMemberIds": [1, 2],
  "participants": [
    {
      "groupMemberId": 1,
      "userId": 1,
      "nickname": "홍길동",
      "confirmedAt": null,
      "selectedDates": []
    }
  ],
  "createdAt": "2026-08-19T07:00:00Z",
  "updatedAt": "2026-08-19T07:10:00Z"
}
```

그룹 멤버만 조회할 수 있다. 현재 일정 상세는 모든 참여자의 `selectedDates`를
포함한다.

확정된 일정은 `confirmedSuggestion`에 확정 장소의 `id`, `name`, `category`,
`address`, `proposedStartAt`, `proposedEndAt`, `reasons`가 포함된다. 그룹 상세 화면에서 확정
일정 카드를 눌렀을 때 이 값을 사용해 완료 요약을 복원한다.

### 작성 중인 일정 삭제

```http
DELETE /api/meetings/{meetingId}
```

일정 생성자가 `DRAFT` 상태에서만 호출할 수 있다. 성공 시 `204 No Content`다.

### 진행 중인 일정 취소

```http
POST /api/meetings/{meetingId}/cancel
```

일정 생성자가 확정 전 일정에 호출할 수 있다. 성공하면 상태가 `CANCELLED`인 전체 일정
상세를 반환한다. 확정 일정 취소와 Google Calendar 삭제는 후속 범위다.

### 일정 조건 전체 교체

```http
PUT /api/meetings/{meetingId}/conditions
```

```json
{
  "purpose": "오랜만에 저녁 식사",
  "region": "건대",
  "scheduleSearchFrom": "2026-08-23",
  "scheduleSearchTo": "2026-09-07",
  "preferredTimeOfDay": "EVENING"
}
```

- Response는 전체 일정 상세 형식이다.
- `DRAFT`에서 일정 생성자만 호출할 수 있다.
- 전체 교체 방식이므로 생략한 필드는 `null`로 저장된다.
- `purpose`와 `region`은 제출 시 필수다.
- 날짜 시작일과 종료일은 조건 저장 요청에서 모두 필수다.
- `preferredTimeOfDay`는 조건 저장 요청에서 필수다. 시간 제약이 없으면 `ANY`를 보낸다.
- `purpose`는 최대 1000자, `region`은 최대 100자다.
- `purpose`는 초안 조건에 저장할 수 있지만, 후보 생성 직전에는 조율 채팅 이력을 AI가 요약한 최종 `purpose`로 갱신한다.

### 일정 참여자 전체 교체

```http
PUT /api/meetings/{meetingId}/participants
```

```json
{ "groupMemberIds": [1, 2, 3] }
```

- 그룹 상세의 `members[].memberId`를 사용한다.
- 한 명 이상 필요하다.
- 해당 그룹 멤버만 선택할 수 있다.
- `DRAFT`에서 일정 생성자만 호출할 수 있다.
- Response는 전체 일정 상세 형식이다.

### 일정 제출

```http
POST /api/meetings/{meetingId}/submit
```

Request Body 없음. 성공하면 전체 일정 상세를 반환하며 상태는 `SURVEYING`이다.

제출 조건:

- 요청자가 일정 생성자
- 현재 상태가 `DRAFT`
- `purpose`, `region` 입력 완료
- 날짜 시작일과 종료일이 모두 입력되고 시작일이 종료일보다 늦지 않음
- `preferredTimeOfDay` 입력 완료 (`상관없음`은 `ANY`)
- 참여자 한 명 이상

현재 재제출은 지원하지 않는다. 제출 후 조건·참여자 수정도 불가능하다.

### 내 가능 날짜 제출

```http
PUT /api/meetings/{meetingId}/my-availability
```

```json
{ "selectedDates": ["2026-08-30", "2026-08-31"] }
```

```json
{
  "groupMemberId": 3,
  "confirmedAt": "2026-08-19T08:00:00Z",
  "selectedDates": ["2026-08-30", "2026-08-31"],
  "meetingStatus": "SURVEYING"
}
```

- 일정에 선택된 참여자 본인만 제출할 수 있다.
- 날짜를 한 개 이상 선택해야 한다.
- 탐색 범위가 있으면 범위 안의 날짜만 허용한다.
- 후보 생성 전인 `SURVEYING`, `READY_TO_PLAN` 상태에서 제출하거나 수정할 수 있다.
- 전원이 제출하면 응답의 `meetingStatus`가 `READY_TO_PLAN`으로 바뀐다.
- 전원이 제출한 후에도 후보 생성 전까지 본인의 날짜를 수정할 수 있다.
- 이전 경로 `/api/meetings/{meetingId}/availability`와 요청 키 `availableDates`도 호환한다.

### 내 가능 날짜 조회

```http
GET /api/meetings/{meetingId}/availability/me
```

응답은 가능 날짜 제출 응답과 같다. 아직 제출하지 않았다면 `confirmedAt`은 `null`,
`selectedDates`는 빈 배열이다.

### 내 Google Calendar 바쁜 날짜 조회

```http
GET /api/meetings/{meetingId}/calendar-busy-dates/me
```

```json
{
  "calendarConnected": true,
  "busyDates": ["2026-08-25", "2026-08-28"]
}
```

일정 참여자 본인만 조회할 수 있다. 일정 제목·장소 등 상세 정보는 읽거나 반환하지 않고,
그룹장이 정한 `scheduleSearchFrom`부터 `scheduleSearchTo` 범위에서 일정 존재 여부만 날짜 단위로 반환한다.
권한이 없으면 `calendarConnected=false`, `busyDates=[]`다.

### 참여자 제출 현황

```http
GET /api/meetings/{meetingId}/coordination
```

```json
{
  "meetingId": 20,
  "status": "SURVEYING",
  "allSubmitted": false,
  "participants": [
    {
      "meetingParticipantId": 10,
      "userId": 1,
      "submitted": true,
      "submittedAt": "2026-08-19T08:00:00Z"
    }
  ]
}
```

그룹 멤버가 조회할 수 있으며 이 API는 다른 참여자의 상세 날짜를 반환하지 않는다.

### 공통 가능 날짜에서 만남 시간 선택

```http
POST /api/meetings/{meetingId}/plan
```

Request Body 없음. `READY_TO_PLAN` 상태에서 일정 생성자만 호출할 수 있다.
백엔드는 모든 참여자의 날짜 교집합을 `YYYY-MM-DD` 문자열 배열로만 AI에 전달한다.
요일 문자열은 전달하지 않는다. AI가 선택한 단일 시간과 이유를 저장하고 전체 일정
상세에 다음 필드를 포함해 반환한다.

```json
{
  "resolvedStartAt": "2026-08-28T10:00:00Z",
  "resolvedEndAt": "2026-08-28T12:00:00Z",
  "scheduleResolutionReason": "금요일 저녁이라 한 주를 마무리하며 여유롭게 만나기 좋아 선택했어요."
}
```

프론트는 별도 결과 페이지를 두지 않고 모임 요청 채팅 상단에 시간과 선정 이유를 표시한다.

## 6. 상태 전이

```text
초안 생성       일정 제출        전원 날짜 제출      조율 시작
DRAFT  ─────▶  SURVEYING  ─────▶ READY_TO_PLAN ─────▶ PLANNING
                                                               │
                                      후보 생성 완료               ▼
                                      CONFIRMED ◀── PROPOSING
```

- 후보 생성이 성공하면 `PROPOSING`, 후보를 확정하면 `CONFIRMED`로 전이한다.
- 후보 재생성을 시작하면 `PROPOSING → READY_TO_PLAN`으로 돌아간다.
- 확정 전 일정은 `POST /api/meetings/{meetingId}/cancel`로 `CANCELLED`처리할 수 있다.
- 한 그룹에 진행 중 일정이 여러 개 생길 수 있다.
- 그룹 응답의 `activeMeetings`는 진행 중인 일정을 최신순으로 모두 반환한다.

## 7. 구현 범위 참고

개인 선호 채팅, 일정 컨텍스트 채팅, 후보 생성·재생성·확정, 일정 취소,
Google Calendar 등록 결과 조회가 구현돼 있다. 알림 수단과 그룹 공지 전송은 후속 범위다.

## 8. 프론트 구현 시 현재 임시 처리

- 온보딩 선호 채팅: 프론트 mock 또는 건너뛰기 후 온보딩 완료 호출
- 모임 목적 챗봇: 프론트에서 목적 문자열을 만든 뒤 `/conditions`의 `purpose`로 저장
- 조율 진행 화면: `/plan` 응답 이후 실제 AI 진행 대신 mock 처리
- 제안·완료 화면: 백엔드 확정 API가 생기기 전까지 mock 처리
- 그룹 멤버 캘린더 연결 여부: 그룹 응답이 아닌 현재 사용자의
  `/api/auth/session.calendarAuthorized`만 사용

## 9. AI 연동 예정 API 계약

이 절의 API는 아직 구현되지 않은 예정 계약이다. 프론트는 아래 HTTP 계약만
사용하고, 백엔드 내부에서 사용하는 AI 모델·프롬프트·응답 형식은 노출하지 않는다.

AI 호출 결과는 신뢰하지 않고 백엔드에서 JSON 스키마 검증, vocabulary 검증,
날짜·시간 범위 검증, 장소 필드 검증을 통과한 데이터만 DB에 저장한다.

### 채택한 백엔드 ↔ AI URI

AI팀이 제공한 규약을 내부 API의 기준으로 사용한다.

| 방향 | Method | URI | 목적 |
| --- | --- | --- | --- |
| Back → AI | `POST` | `/ai/preferences/extract` | 개인 선호 추출과 사용자 답변 생성 |
| Back → AI | `POST` | `/ai/meetings/{meetingId}/schedule` | 공통 가능 날짜에서 단일 만남 일시 선택 |
| Back → AI | `POST` | `/ai/meetings/{meetingId}/context/messages` | 멀티턴 목적 채팅과 후보 날짜 변경 |
| Back → AI | `POST` | `/ai/meetings/{meetingId}/context` | 모임 목적 한 문장 정리 |
| Back → AI | `POST` | `/ai/meetings/{meetingId}/candidates` | 시간·장소 후보 최대 3개 생성 |
| Back → AI | `POST` | `/ai/meetings/{meetingId}/revise` | 재생성 대화 한 턴 처리 |
| Back → AI | `GET` | `/health` | AI 프로세스 생존 확인 |
| AI → Back | `GET` | `/internal/preference-vocabulary` | 전체 선호 Vocabulary 조회 |

`/internal/preference-vocabulary`의 구현·캐시 정책과 내부 인증 방식은 추후 확정한다.
프론트는 위 내부 URI를 직접 호출하지 않는다.

후보 생성 규칙은 다음과 같이 확정한다.

- `scheduleSearchFrom`, `scheduleSearchTo`는 필수다.
- `preferredTimeOfDay`는 필수이며 `DAYTIME`, `LATE_AFTERNOON`, `EVENING`, `ANY` 중 하나를 보낸다.
- 모임 길이는 입력받지 않고 MVP에서 항상 `durationMinutes=120`을 보낸다.
- 시간대 범위는 `DAYTIME 11:00~15:00`, `LATE_AFTERNOON 15:00~18:00`,
  `EVENING 18:00~23:00`, `ANY 11:00~23:00`을 사용한다.

### 개인 선호 채팅

```http
POST /api/users/me/preferences/chat
```

```json
{
  "message": "매운 음식 좋아해"
}
```

```json
{
  "reply": "말씀해주신 내용을 선호에 반영했어요.",
  "preferences": [
    {
      "id": 31,
      "vocabularyCode": "SPICY_FOOD",
      "displayName": "매운 음식",
      "domain": "FOOD",
      "rawValue": "매운 음식",
      "sentiment": "POSITIVE",
      "strength": "MODERATE",
      "mappingType": "EXACT"
    }
  ]
}
```

- 로그인한 본인의 선호만 변경할 수 있다.
- 추출 결과는 `(user_id, vocabulary_code)` 기준 UPSERT한다.
- `UNMAPPED` 결과도 원문 보존을 위해 저장하며 Vocabulary 관련 필드는 `null`이다.
- 빈 `message`는 `400 Bad Request`다.
- AI 응답 파싱 또는 검증 실패는 `502 AI_RESPONSE_INVALID`다.

### 모임 컨텍스트 채팅 (Front ↔ Back)

```http
POST /api/meetings/{meetingId}/chat/messages
```

```json
{
  "message": "다른 날로 바꾸고 싶어요"
}
```

```json
{
  "reply": "네, 30일로 바꾸드릴게요.",
  "candidateDates": [
    { "date": "2026-08-23", "selected": false },
    { "date": "2026-08-30", "selected": true }
  ],
  "resolvedStartAt": "2026-08-30T09:00:00Z",
  "resolvedEndAt": "2026-08-30T11:00:00Z"
}
```

- 공통 가능 날짜가 모두 제출되고 `/schedule`이 완료된 `READY_TO_PLAN` 상태에서
  일정 생성자만 호출할 수 있다.
- 프론트는 사용자의 현재 메시지만 보낸다. 백엔드가 저장된 전체 이력과 후보 날짜를
  AI 요청으로 조립한다.
- `candidateDates`의 `selected=true`가 바뀌면 응답에 새 선택과 계산된 시작·종료 시각이
  포함된다. 시각은 `preferredTimeOfDay`와 120분 규칙으로 계산한다.
- 원문은 `meeting_chat_messages`에 순서대로 저장하고, 후보 생성 직전 `/context`로
  요약한 최종 목적을 `meetings.purpose`에 저장한다.

### 모임 컨텍스트 채팅 (Back ↔ AI)

```http
POST /ai/meetings/{meetingId}/context/messages
```

```json
{
  "history": [
    { "role": "USER", "content": "오랜만에 만나요" },
    { "role": "ASSISTANT", "content": "어떤 분위기를 원하세요?" }
  ],
  "message": "다른 날로 바꾸고 싶어요",
  "candidateDates": [
    { "date": "2026-08-23", "selected": true },
    { "date": "2026-08-30", "selected": false }
  ]
}
```

AI는 `reply`와 같은 날짜 목록을 반환하며, 날짜를 바꿨다면 `selected` 위치만 이동시킨다.
백엔드는 매 턴 전체 목록을 다시 보내고, 중복 없음·동일한 날짜 집합·단 하나의 선택을
검증한다. 날짜 변경은 `meetings.resolved_start_at/resolved_end_at`을 갱신한다.
`confirmed_start_at/confirmed_end_at`은 장소 후보를 최종 확정할 때만 갱신한다.

### AI 조율 실행

```http
POST /api/meetings/{meetingId}/plan
Idempotency-Key: {UUID}
```

Request Body 없음. 예정 계약에서는 비동기 작업을 만들고 `202 Accepted`를 반환한다.

```json
{
  "runId": 101,
  "meetingId": 20,
  "status": "QUEUED",
  "createdAt": "2026-08-19T08:10:00Z"
}
```

- `READY_TO_PLAN` 상태에서 일정 생성자만 호출할 수 있다.
- 동일 `Idempotency-Key` 재요청은 같은 `runId`를 반환해 중복 AI 호출을 막는다.
- 실행 생성과 동시에 일정 상태는 `PLANNING`으로 바뀐다.
- 현재 구현은 임시로 동기 `200 MeetingResponse`를 반환하므로 AI 연동 시 위 계약으로 변경한다.

### AI 조율 진행 상태 조회

```http
GET /api/meetings/{meetingId}/agent-runs/{runId}
```

```json
{
  "runId": 101,
  "meetingId": 20,
  "status": "RUNNING",
  "currentStep": "SEARCHING_PLACES",
  "steps": [
    { "code": "CALCULATING_OVERLAP", "status": "COMPLETED" },
    { "code": "SUMMARIZING_PREFERENCES", "status": "COMPLETED" },
    { "code": "DETERMINING_PLACE_TYPE", "status": "COMPLETED" },
    { "code": "SEARCHING_PLACES", "status": "RUNNING" },
    { "code": "VERIFYING_BUSINESS_INFO", "status": "PENDING" }
  ],
  "error": null,
  "createdAt": "2026-08-19T08:10:00Z",
  "updatedAt": "2026-08-19T08:10:05Z"
}
```

```text
AgentRunStatus: QUEUED | RUNNING | SUCCEEDED | FAILED
AgentRunStepStatus: PENDING | RUNNING | COMPLETED | FAILED
```

- MVP는 1~2초 간격 polling을 사용한다.
- 성공하면 일정 상태는 `PROPOSING`, 실패하면 `FAILED`로 전환한다.
- 실패 응답의 `error`는 `{ "code", "message", "retryable" }` 형태다.
- SSE는 polling으로 성능 문제가 확인될 때 추가한다.

### 제안 목록 조회

```http
GET /api/meetings/{meetingId}/suggestions
```

```json
{
  "meetingId": 20,
  "status": "PROPOSING",
  "summary": "대화하기 좋은 저녁 식사 장소를 우선했어요.",
  "suggestions": [
    {
      "id": 501,
      "rank": 1,
      "category": "음식점",
      "name": "건대 예시 식당",
      "address": "서울 광진구 예시로 1",
      "latitude": 37.5401,
      "longitude": 127.0692,
      "externalUrl": "https://place.map.kakao.com/12345",
      "proposedStartAt": "2026-08-30T19:00:00+09:00",
      "proposedEndAt": "2026-08-30T21:00:00+09:00",
      "businessHoursVerified": true,
      "openAtMeetingTime": true,
      "reasons": ["그룹 선호 적합", "모임 시간 이용 가능"],
      "sourceUrls": ["https://example.com/place"],
      "checkedAt": "2026-08-19T08:11:00Z"
    }
  ]
}
```

- 일정이 `PROPOSING`일 때 그룹 멤버가 조회할 수 있다.
- 확인되지 않은 영업 정보는 `null` 또는 `false`로 반환하고 추측해 채우지 않는다.
- 개인별 선호는 노출하지 않고 그룹 단위 추천 이유만 반환한다.
- 외부 장소 ID, 출처 URL, 확인 시각을 저장해 재검증할 수 있어야 한다.

### 재생성 대화 한 턴

```http
POST /api/meetings/{meetingId}/revision/chat
```

```json
{
  "messages": ["조금 더 조용하고 가격이 낮은 곳으로 찾아줘"]
}
```

```json
{
  "revisionId": 30,
  "reply": "조금 더 조용하고 가격이 낮은 장소로 다시 찾을게요. 이 조건으로 진행할까요?",
  "draftPurpose": "가격 부담이 적고 조용한 장소에서 대화하는 저녁 모임",
  "excludedExternalPlaceIds": [],
  "uiChangeRequests": []
}
```

- `PROPOSING` 상태에서 일정 생성자만 호출할 수 있다.
- `messages`에는 이번 턴에 새로 입력한 문장만 보낸다.
- 백엔드는 현재 목적, 현재 제안, 누적 제외 장소를 조회해 AI의
  `/ai/meetings/{meetingId}/revise` 요청으로 조립한다.
- AI는 상태를 저장하지 않으며 백엔드가 `revisionId`별 초안과 대화 이력을 저장한다.
- 이 API는 후보를 생성하지 않고 대화 초안만 갱신한다.

### 재생성 조건 최종 적용

```http
POST /api/meetings/{meetingId}/revisions/{revisionId}/apply
Idempotency-Key: {UUID}
```

AI가 지역·날짜·시간 변경 확인을 요청하지 않았다면 Request Body는 없거나
`conditionChanges`를 `null`로 보낸다. 사용자가 UI에서 변경을 확인했다면 정규화된
값을 전달한다.

```json
{
  "conditionChanges": {
    "region": "성수",
    "scheduleSearchFrom": "2026-08-30",
    "scheduleSearchTo": "2026-09-06",
    "preferredTimeOfDay": "ANY"
  }
}
```

Response `202 Accepted`:

```json
{
  "runId": 102,
  "meetingId": 20,
  "status": "QUEUED",
  "createdAt": "2026-08-19T08:20:00Z"
}
```

- `PROPOSING` 상태에서 일정 생성자만 호출할 수 있다.
- 해당 일정의 아직 적용되지 않은 `revisionId`만 사용할 수 있다.
- 최종 목적, 확정된 UI 조건, 제외 장소를 반영해 AI의 `/candidates`를 호출한다.
- 기존 제안을 덮어쓰지 않고 세대(generation)를 구분해 보존한다.
- 일정 상태는 다시 `PLANNING`으로 변경한다.
- 동일 `Idempotency-Key`는 같은 `runId`를 반환한다.

### 제안 확정

```http
POST /api/meetings/{meetingId}/suggestions/{suggestionId}/confirm
Idempotency-Key: {UUID}
```

Request Body 없음.

```json
{
  "meetingId": 20,
  "suggestionId": 501,
  "status": "CONFIRMED",
  "confirmedStartAt": "2026-08-30T19:00:00+09:00",
  "confirmedEndAt": "2026-08-30T21:00:00+09:00",
  "place": {
    "name": "건대 예시 식당",
    "address": "서울 광진구 예시로 1",
    "externalUrl": "https://place.map.kakao.com/12345"
  }
}
```

- `PROPOSING` 상태에서 일정 생성자만 호출할 수 있다.
- 해당 일정의 현재 generation에 속한 제안만 확정할 수 있다.
- 확정과 `meetings` 상태·일시 갱신은 하나의 DB 트랜잭션으로 처리한다.
- Calendar 등록은 확정 트랜잭션과 분리하고 참여자별 성공·실패를 별도로 기록한다.

### 백엔드에서 AI 서비스로 전달할 내부 계약

프론트는 이 JSON을 만들거나 전달하지 않는다. 백엔드가 DB 데이터를 읽어 AI 호출용
DTO로 조립한다. 모델 변경에 대비해 `contractVersion`을 포함한다.

```json
{
  "contractVersion": "1.0",
  "requestId": "6e214a43-56a6-4b3b-a63c-14a1d3bb3c72",
  "meeting": {
    "id": 20,
    "purpose": "오랜만에 만나 대화하는 저녁 식사",
    "region": "건대",
    "scheduleSearchFrom": "2026-08-23",
    "scheduleSearchTo": "2026-09-07",
    "preferredTimeOfDay": "EVENING",
    "durationMinutes": 120,
    "timezone": "Asia/Seoul"
  },
  "participants": [
    {
      "userId": 1,
      "selectedDates": ["2026-08-30"],
      "preferences": [
        {
          "vocabularyCode": "SPICY_FOOD",
          "sentiment": "POSITIVE",
          "strength": "MODERATE",
          "rawValue": "매운 음식"
        }
      ]
    }
  ],
  "meetingMemory": {},
  "revisionMessages": []
}
```

백엔드는 AI 응답을 그대로 프론트에 전달하지 않는다. 내부 응답을 검증하고 정규화한
뒤 `meeting_suggestions` 등에 저장하고, 프론트에는 위의 제안 목록 계약만 반환한다.

AI 후보 결과가 `NO_COMMON_SLOT`, `NO_CANDIDATE`, `CONFLICT`인 경우는 서버 장애가
아니다. 제안은 저장하지 않고 일정을 `READY_TO_PLAN`으로 되돌려 사용자가 날짜·지역·
목적을 수정하거나 다시 실행할 수 있게 한다. 네트워크·모델·스키마 오류는 agent run을
`FAILED`로 기록하되 일정 역시 재시도할 수 있도록 `READY_TO_PLAN`으로 복구한다.

## 10. 기존 문서 대비 변경사항

| 구분 | 기존 문서 | 현재 계약 |
| --- | --- | --- |
| 사용자 인증 | `X-User-Id` 헤더 또는 ID Token 직접 전달 | Google OAuth 서버 세션과 `JSESSIONID` 사용 |
| Google 로그인 | `POST /api/auth/google` | `GET /api/auth/google`로 페이지 이동 |
| 상태 변경 요청 | 별도 CSRF 안내 없음 | `POST`, `PUT`, `PATCH`, `DELETE`에 CSRF 헤더 필수 |
| 그룹 가입 상태 | `group_members.status=JOINED` | 상태 컬럼 제거. 멤버 행 존재 자체가 가입 상태 |
| 그룹 생성 응답 | 멤버에 `status: JOINED` 포함 | `status` 제거, `memberCount`, `pastMeetingCount`, `preferenceCount`, `calendarConnected`, `activeMeeting` 포함 |
| 중복 그룹 가입 | 충돌 오류 또는 재가입 처리 검토 | `200 OK`, `alreadyMember=true` 반환 |
| 그룹 멤버 식별자 | 문서에 `groupMemberId`와 `memberId` 혼용 | 그룹 상세은 `memberId`, 가입 응답은 `groupMemberId`; 참여자 요청에는 그룹 상세의 `memberId` 사용 |
| 그룹 목록 | 기본 그룹 정보만 반환 | `memberCount`, `members`, `lastMeeting`, `activeMeeting` 포함 |
| 그룹 상세 | 참여자와 기본 정보 | `pastMeetingCount`, `preferenceCount`, `calendarConnected`, 최신 `activeMeeting` 포함 |
| 다가오는 일정 | 참여자 배열 포함 | 현재 `participants` 없이 일정 기본 정보만 반환 |
| 다가오는 일정 없음 | JSON `null` | 현재 `200 OK` 빈 본문 |
| 일정 참여자 저장 응답 | `{ "groupMemberIds": [...] }` | 전체 `MeetingResponse` 반환 |
| 일정 조건 저장 응답 | 변경한 조건만 반환 | 전체 `MeetingResponse` 반환 |
| 일정 조건 요청 | `purpose`를 context-chat에서 저장 | 초안은 `/conditions`, 최종 목적은 조율 채팅 요약으로 갱신 |
| 선호조사 마감일 | `preferenceSurveyDeadline` 사용 | 필드 및 DB 컬럼 제거 |
| 일정 수집 상태 | `COLLECTING_AVAILABILITY` 별도 상태 | 제거하고 제출 직후 `SURVEYING` 사용 |
| 일정 상태 전이 | 마감일에 따라 `SURVEYING` 또는 `READY_TO_PLAN` | `DRAFT → SURVEYING → READY_TO_PLAN → PLANNING` |
| 날짜 탐색 범위 | 선택 가능 | 일정 초안 자체는 비어 있지만 `/conditions` 요청에는 시작일·종료일 모두 필수 |
| 가능 날짜 API | `/availability`, `availableDates` | `/my-availability`, `selectedDates`; 기존 이름도 호환 |
| 가능 날짜 저장 | `selected_dates DATE[]` 제안 | `meeting_available_dates` 별도 테이블 사용 |
| 가능 날짜 수정 | 재제출 가능 여부 불명확 | 후보 생성 전 `SURVEYING`, `READY_TO_PLAN`에서 본인 날짜 수정 가능 |
| 일정 재제출 | 확인 상태 초기화 후 재제출 가정 | 현재 재제출 및 제출 후 조건 수정 미지원 |
| 내 선호 조회 | 계약만 존재 | `GET /api/users/me/preferences` 구현 완료 |
| 선호 채팅 | 구현된 API로 표기 | `{message}`를 받아 AI 추출 후 UPSERT 또는 `UNMAPPED` 원문 저장 |
| 모임 context-chat | 날짜와 분리된 목적 채팅 | `/chat/messages`에서 멀티턴을 저장하며 후보 날짜 변경도 처리 |
| AI 조율 | `/plan`에서 AI 실행 | 컨텍스트 요약, AI 후보 생성, 저장, `PROPOSING` 전환을 동기 처리 |
| 제안·확정·캘린더·알림 | 추후 구현 예정 | 현재 모두 미구현 |
| AI API 공개 범위 | AI 원본 응답을 프론트가 직접 파싱할 가능성 | 프론트는 백엔드 고정 DTO만 사용하고 AI 원본 응답은 백엔드가 검증·정규화 |
| AI 실행 방식 | `/plan` 단일 동기 요청 | 예정 계약은 `202 + runId`, 진행 상태 polling, `Idempotency-Key` 사용 |
| AI 실패 처리 | 발생 조건과 응답 미정 | `AgentRunStatus`, 단계별 상태, 재시도 가능 여부를 구조화해 반환 |
| 개인 선호 채팅 | 엔드포인트 형태만 존재 | 요청·응답, UPSERT, 검증 실패 오류 계약 추가 |
| 모임 목적 채팅 | 목적 문자열 응답만 정의 | 생성자 권한, 원문 저장, 목적·장기기억 갱신 경계 추가 |
| 제안 목록 | 장소 필드와 근거가 미정 | 시간·장소·검증 상태·출처·조회 시각을 포함한 고정 DTO 추가 |
| 재생성 | 단일 요청으로 대화와 후보 생성 | `/revision/chat`으로 멀티턴 대화 후 `/revisions/{revisionId}/apply`로 최종 실행 |
| 제안 확정 | 상태 변경 예정 | 생성자 권한, 현재 generation 검증, DB 트랜잭션 경계 추가 |
| 백엔드→AI 계약 | DB 데이터를 그대로 전달하는 방향 | `contractVersion`이 있는 내부 DTO로 조립하고 응답 스키마 검증 후 저장 |
| 모임 길이 | 입력 및 기본값 미정 | MVP에서 120분 고정, AI 요청에 `durationMinutes=120` 전달 |
| AI 선호 시간대 | 필수 여부 불명확 | 필수. 시간 제약이 없으면 `ANY(11:00~23:00)`를 전달 |
| AI 결과 없음 | 실패 상태 처리 미정 | `NO_COMMON_SLOT`, `NO_CANDIDATE`, `CONFLICT`는 `READY_TO_PLAN`으로 복구 |
