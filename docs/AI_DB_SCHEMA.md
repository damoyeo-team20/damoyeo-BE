# Damoyeo DB Schema 공유본 (AI 팀용)

AI 서버가 DB에 직접 접근하지 않고, 백엔드가 DB 데이터를 조회해 AI 요청 DTO로 전달합니다.

## 현재 구현된 관계

```text
users
  └─ group_members
       └─ meeting_groups
            └─ meetings
                 └─ meeting_participants
```

## users

사용자 기본 정보입니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 사용자 ID |
| `google_subject` | VARCHAR(255) UNIQUE | Google 계정 고정 식별자 |
| `email` | VARCHAR(320) UNIQUE | 이메일 |
| `nickname` | VARCHAR(50), nullable | 닉네임 |
| `onboarding_completed` | BOOLEAN | 선호 온보딩 완료 여부 |
| `created_at` | TIMESTAMPTZ | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | 수정 시각 |

## meeting_groups

반복해서 유지되는 모임 그룹입니다. 한 그룹 안에서 여러 일정이 생성됩니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 그룹 ID |
| `name` | VARCHAR(100) | 그룹 이름 |
| `invite_code` | VARCHAR(8) UNIQUE | 그룹 초대 코드 |
| `created_at` | TIMESTAMPTZ | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | 수정 시각 |

## preference_vocabulary

Agent가 사용할 수 있는 선호 코드와 계층입니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | Vocabulary ID |
| `code` | VARCHAR(100) UNIQUE | `MEAT`, `SEAFOOD` 등 |
| `domain` | VARCHAR(50) | `FOOD`, `ACTIVITY` 등 |
| `parent_code` | VARCHAR(100), self FK | 상위 Vocabulary 코드 |
| `display_name` | VARCHAR(100) | UI 표시 이름 |

## user_preferences

사용자별 Vocabulary 코드의 최신 선호입니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 선호 ID |
| `user_id` | BIGINT FK | `users.id` |
| `vocabulary_code` | VARCHAR(100) FK | `preference_vocabulary.code` |
| `raw_value` | VARCHAR(255) | 사용자가 실제 언급한 표현 |
| `sentiment` | VARCHAR(20) | `POSITIVE`, `NEGATIVE` |
| `strength` | VARCHAR(20) | `WEAK`, `MODERATE`, `STRONG` |
| `mapping_type` | VARCHAR(20) | `EXACT`, `GENERALIZED`, `UNMAPPED` |
| `source_text` | TEXT | 입력 원문 전체 |
| `created_at` | TIMESTAMPTZ | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | 수정 시각 |

`(user_id, vocabulary_code)`는 UNIQUE이며 동일 코드가 다시 입력되면 UPSERT합니다.

## group_members

그룹에 속한 사용자입니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 그룹 멤버 ID |
| `group_id` | BIGINT FK | `meeting_groups.id` |
| `user_id` | BIGINT FK | `users.id` |
| `role` | VARCHAR(20) | `HOST`, `MEMBER` |
| `status` | VARCHAR(20) | `INVITED`, `JOINED`, `DECLINED` |
| `joined_at` | TIMESTAMPTZ, nullable | 가입 시각 |
| `created_at` | TIMESTAMPTZ | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | 수정 시각 |

제약조건:

- 한 그룹에서 같은 사용자는 한 번만 가입 가능
- 그룹당 `HOST`는 한 명

## meetings

그룹 안에서 매번 새로 조율하는 개별 일정입니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 일정 ID |
| `group_id` | BIGINT FK | `meeting_groups.id` |
| `created_by` | BIGINT FK | 일정을 생성한 `users.id` |
| `purpose` | VARCHAR(1000), nullable | 이번 모임 목적 자연어 |
| `region` | VARCHAR(100), nullable | 장소 검색 지역 |
| `schedule_search_from` | DATE, nullable | 일정 탐색 시작일 |
| `schedule_search_to` | DATE, nullable | 일정 탐색 종료일 |
| `preferred_time_of_day` | VARCHAR(30), nullable | 선호 시간대 |
| `status` | VARCHAR(30) | 일정 진행 상태 |
| `confirmed_start_at` | TIMESTAMPTZ, nullable | 최종 확정 시작 시각 |
| `confirmed_end_at` | TIMESTAMPTZ, nullable | 최종 확정 종료 시각 |
| `created_at` | TIMESTAMPTZ | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | 수정 시각 |

`preferred_time_of_day`:

```text
DAYTIME | LATE_AFTERNOON | EVENING | ANY
```

`status`:

```text
DRAFT | SURVEYING | READY_TO_PLAN | PLANNING
PROPOSING | CONFIRMED | FAILED | CANCELLED
```

선호 변경 마감일 당일까지는 AI 제안 생성을 요청할 수 없습니다. 마감일이 지난 후 일정 생성자가 AI 채팅으로 조율을 요청합니다.

## meeting_participants

이번 일정에 실제로 참여하는 그룹 멤버입니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 일정 참여자 ID |
| `meeting_id` | BIGINT FK | `meetings.id` |
| `group_member_id` | BIGINT FK | `group_members.id` |
| `created_at` | TIMESTAMPTZ | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | 수정 시각 |

한 일정에 같은 그룹 멤버를 중복 등록할 수 없습니다.

## meeting_chat_messages

사용자와 AI가 실제로 주고받은 대화 원문입니다. 메시지 한 건당 한 행을 저장합니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 메시지 ID |
| `meeting_id` | BIGINT FK | `meetings.id` |
| `role` | VARCHAR(20) | `USER`, `ASSISTANT` |
| `content` | TEXT | 실제 표시된 메시지 원문 |
| `created_at` | TIMESTAMPTZ | 메시지 생성 시각 |

## meeting_memories

일정별 전체 대화를 압축한 구조화 기억입니다. 일정당 한 행을 저장합니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `meeting_id` | BIGINT PK/FK | `meetings.id` |
| `memory` | JSONB | 요약, 선호·회피 패턴, 최종 결정 등 |
| `updated_at` | TIMESTAMPTZ | 마지막 압축 시각 |

다음 일정 조율 시에는 과거 대화 원문 전체가 아니라 같은 그룹의 `meeting_memories.memory`를 우선 전달합니다.

## AI 요청 시 백엔드 조회 흐름

```text
meetings
→ meeting_participants
→ group_members
→ users
→ user_preferences
→ preference_vocabulary
→ Calendar 가능 시간 (외부 연동)
```

백엔드가 AI 서버에 전달할 예정인 형태:

```json
{
  "meetingId": 1,
  "purpose": "오랜만에 만나서 저녁 식사",
  "region": "건대",
  "scheduleSearchFrom": "2026-08-23",
  "scheduleSearchTo": "2026-09-07",
  "preferredTimeOfDay": "EVENING",
  "participants": [
    {
      "userId": 1,
      "preferences": [],
      "calendarAvailability": []
    }
  ]
}
```

## 아직 구현되지 않은 테이블

다음 테이블은 AI 입출력 계약 확정 후 추가할 예정입니다.

```text
agent_runs
meeting_suggestions
revision_requests
meeting_calendar_events
```

특히 `preferences`, `calendarAvailability`, AI 응답 및 제안 후보의 JSON 구조는 아직 확정되지 않았습니다.
