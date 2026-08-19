# Damoyeo API 명세

## 공통

```text
Base URL: http://localhost:8080/api
X-User-Id: 1
Content-Type: application/json
```

OAuth 연동 전까지 모든 요청에 임시 `X-User-Id` 헤더가 필요합니다.
헤더 값은 DB에 존재하는 사용자 ID여야 합니다.

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

- 조사 마감일이 오늘 또는 미래: `SURVEYING`
- 조사 마감일이 없거나 지남: `READY_TO_PLAN`

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
DRAFT | SURVEYING | READY_TO_PLAN | PLANNING
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

- 초대 코드 참여, OAuth, Preference
- 실제 AI 채팅 및 조율
- 장소 제안·재생성·확정
- Google Calendar 등록
