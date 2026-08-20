# 다모여 AI 팀 전달용 — 일정 조율·후보 생성 계약 v2

## 1. 제품 플로우

1. 일정 생성자가 참여자·지역·날짜 탐색 범위·희망 시간대를 입력한다.
2. 모든 참여자가 가능한 날짜를 제출하면 일정 상태는 `READY_TO_PLAN`이 된다.
3. 이때부터 **일정 생성자만** 일정 조율 채팅을 여러 턴 진행한다.
4. 매 채팅 턴에서 Back은 새 사용자 메시지와 이전의 압축 컨텍스트를 AI에 전달한다. AI는 사용자용 답변과 갱신된 압축 컨텍스트를 반환한다.
5. 사용자가 `후보 생성`을 누르면 Back은 최신 컨텍스트, 확정된 지역·시간 조건, 가능 날짜, 개인 선호, 그룹 장기 기억을 AI에 전달한다. AI는 Kakao Local 등을 이용해 후보 1~3개를 반환한다.
6. 후보가 표시된 상태는 `PROPOSING`이다. `다시 생성하기`를 누르면 `READY_TO_PLAN`으로 돌아가며, 기존 후보는 제외 목록에 넣고 같은 채팅을 이어간다.
7. 후보 하나를 확정하면 `CONFIRMED`가 되며, Back은 확정 사실과 기존 그룹 요약을 AI에 보내 새 그룹 장기 요약을 생성한다. 확정 뒤에는 변경하지 않는다.

AI는 상태를 저장하지 않는다. 원문 채팅, 일정 단기 컨텍스트, 후보 이력, 그룹 장기 컨텍스트는 모두 Back DB가 관리한다.

## 2. 기억 구분

| 구분 | Back 저장 위치 | AI 사용 시점 |
| --- | --- | --- |
| 개인 장기 선호 | `user_preferences` | 후보 생성 |
| 일정 채팅 원문 | `meeting_chat_messages` | 화면 표시·필요시 재해석 |
| 일정 단기 컨텍스트 | `meeting_memories.memory.meetingContext` | 채팅 매 턴·후보 생성 |
| 제외 장소 | `meeting_memories.memory.excludedExternalPlaceIds` | 재생성 후보 생성 |
| 후보 이력·확정 후보 | `meeting_suggestions`, `meetings.confirmed_suggestion_id` | 제안 표시·확정 |
| 그룹 장기 기억 | `group_memories.summary` | 다음 일정 후보 생성 |

그룹 장기 요약에는 개인 이름이나 특정 사용자의 선호를 넣지 않는다. 그룹 수준의 확정 이력만 압축한다.

## 3. Back → AI 공통 규칙

- `Content-Type: application/json`
- `camelCase`
- `messages`에는 **이번 턴의 새 문장만** 넣는다.
- 날짜는 `YYYY-MM-DD`, 시간은 timezone offset 포함 ISO 8601이다.
- AI의 응답 배열은 값이 없으면 `[]`이다.
- 내부 인증은 `X-Internal-Api-Key` 공유 키를 사용한다.

## 4. 일정 조율 채팅

```http
POST /ai/meetings/{meetingId}/chat
```

요청:

```json
{
  "messages": ["조용하고 가성비 좋은 곳이면 좋겠어"],
  "currentContext": null,
  "currentSuggestions": [],
  "excludedExternalPlaceIds": []
}
```

`currentContext`는 첫 턴에는 `null`이고, 이후 Back이 직전 `updatedContext`를 넣는다. 지역·날짜·시간대는 이미 일정 조건으로 확정돼 있으므로 채팅에서 변경하지 않는다.

응답:

```json
{
  "reply": "조용하고 가격 부담이 적은 곳을 우선으로 볼게요. 더 반영할 조건이 있으면 알려주세요!",
  "updatedContext": "조용하고 가성비 좋은 분위기의 식사 모임",
  "excludedExternalPlaceIds": [],
  "uiChangeRequests": []
}
```

- `reply`: 사용자에게 보여줄 자연어 답변. 기본은 반영 안내와 추가 조건 요청이며, 반드시 질문일 필요는 없다.
- `updatedContext`: 1,000자 이하. Back이 다음 채팅·후보 생성에 쓰는 압축본이다.
- `currentSuggestions`: 후보 생성 후 재생성 채팅일 때만 현재 후보를 넣는다. 각 원소는 `rank`, `externalPlaceId`, `name`, `category`, `proposedStartAt`, `proposedEndAt`, `reasons`를 가진다.
- `excludedExternalPlaceIds`: 사용자가 후보 제외를 요청하면 최신 누적 목록을 반환한다.
- `uiChangeRequests`: `REGION`, `DATE`, `TIME` 변경을 언급했을 때만 사용한다. Back은 별도 UI 확인 없이는 일정 조건을 바꾸지 않는다.

## 5. 후보 생성

```http
POST /ai/meetings/{meetingId}/candidates
```

```json
{
  "contractVersion": "2.0",
  "requestId": "UUID",
  "meeting": {
    "id": 20,
    "purpose": "최신 meetingContext",
    "region": "건대",
    "scheduleSearchFrom": "2026-08-23",
    "scheduleSearchTo": "2026-09-07",
    "preferredTimeOfDay": "EVENING",
    "durationMinutes": 120,
    "timezone": "Asia/Seoul"
  },
  "participants": [],
  "meetingMemory": { "meetingContext": "..." },
  "groupMemory": { "summary": "그룹의 확정 만남 기반 장기 요약" },
  "excludedExternalPlaceIds": ["카카오장소ID"]
}
```

후보는 최대 3개다. 장소 검색은 AI 서버가 Kakao Local API를 호출해 실제 장소 ID·이름·주소·좌표·상세 URL을 채운다. 다시 생성 시에는 직전 generation의 후보 장소 ID 전체가 제외 목록으로 전달된다.

`OK` 응답의 후보는 기존 계약의 `CandidateSuggestion` 형식을 유지한다. `NO_COMMON_SLOT`, `NO_CANDIDATE`, `CONFLICT`는 HTTP 200의 업무 결과다.

## 6. 그룹 장기 기억 갱신

후보 확정 직후 Back이 호출한다.

```http
POST /ai/groups/{groupId}/memory
```

```json
{
  "previousGroupSummary": "이 그룹은 조용한 저녁 식사와 대화 중심의 만남을 자주 확정했다.",
  "confirmedMeeting": {
    "meetingId": 20,
    "region": "건대",
    "category": "한식",
    "placeName": "예시 식당",
    "address": "서울 광진구 예시로 1",
    "startAt": "2026-08-30T19:00:00+09:00",
    "endAt": "2026-08-30T21:00:00+09:00",
    "meetingContext": "조용하고 가성비 좋은 분위기의 식사 모임"
  }
}
```

첫 확정이면 `previousGroupSummary`는 `null`이다.

응답:

```json
{
  "updatedGroupSummary": "이 그룹은 건대·성수에서 조용하고 가격 부담이 적은 저녁 식사와 대화 중심 모임을 자주 확정했다."
}
```

- `updatedGroupSummary`: 2,000자 이하.
- 개인 이름·개인의 민감 선호를 언급하지 않는다.
- Back은 이 값을 `group_memories.summary`에 UPSERT한다.

## 7. Back 공개 API (프론트 참고)

```text
GET  /api/meetings/{meetingId}/chat/messages
POST /api/meetings/{meetingId}/chat/messages
POST /api/meetings/{meetingId}/generate
GET  /api/meetings/{meetingId}/suggestions
POST /api/meetings/{meetingId}/regenerate
POST /api/meetings/{meetingId}/confirm
```

`regenerate`는 `PROPOSING → READY_TO_PLAN` 전이만 수행한다. 이후 채팅을 계속하고 `generate`를 다시 호출한다.
