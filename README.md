# Damoyeo Backend

여러 사용자의 선호와 일정을 바탕으로 모임의 시간, 활동, 장소를 조율하는 Damoyeo 서비스의 백엔드입니다.

현재 저장소에는 그룹·미팅·선호 도메인과 Google OAuth 로그인이 구현되어 있습니다.

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Gradle 9.5.1 (Wrapper)
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- Flyway
- PostgreSQL 17
- JUnit 5 / H2 (테스트)

## 사전 준비

- Docker 및 Docker Compose

GHCR의 이미지를 사용하면 JDK나 Gradle을 설치할 필요가 없습니다. 백엔드 소스를 직접 수정해 실행하려면 JDK 21이 추가로 필요합니다.

## 시작하기

백엔드 이미지와 PostgreSQL을 함께 실행합니다. `.env`는 필수가 아니며, 로컬 검증에 필요한 값은 Compose에 기본값으로 정의되어 있습니다.

```bash
# compose.yml이 있는 디렉터리에서
docker compose pull
docker compose up -d
docker compose ps
```

로컬 검증을 인증 없이 실행하려면 `ghcr.io/damoyeo-team20/damoyeo-be` 패키지가 public이어야 합니다. `docker compose pull`에서 `unauthorized`가 발생하는 private 패키지라면 `read:packages` 권한으로 GHCR에 먼저 로그인합니다.

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u <github-username> --password-stdin
```

두 서비스가 `healthy`가 되면 아래 주소에서 상태를 확인할 수 있습니다.

```text
http://localhost:8080/actuator/health
```

### AI/FastAPI 로컬 연동

FastAPI에서 백엔드와 DB에 접속할 때는 다음 로컬 주소를 사용합니다.

```dotenv
BACKEND_BASE_URL=http://localhost:8080
DATABASE_URL=postgresql://damoyeo:damoyeo@localhost:5432/damoyeo
INTERNAL_API_KEY=local-ai-key
```

`/internal/**` API를 호출할 때는 `X-Internal-Api-Key: local-ai-key` 헤더를 보냅니다. DB는 FastAPI에서 직접 접속할 수 있지만, 도메인 데이터를 변경하는 작업은 가능하면 백엔드 API를 통해 실행해야 됩니다.

백엔드가 로컬 FastAPI를 호출할 수 있도록 FastAPI는 기본적으로 `0.0.0.0:8000`에 바인딩합니다.

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Compose는 이 서버를 `http://host.docker.internal:8000`으로 호출합니다. FastAPI의 포트가 다르면 `.env`의 `AI_PORT`를 변경합니다.

### 백엔드 소스 개발

소스를 직접 실행할 때는 DB만 띄운 뒤 Gradle Wrapper를 사용합니다.

```bash
cp .env.example .env
docker compose up -d postgres
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```

테스트는 별도의 PostgreSQL 없이 H2 인메모리 DB를 사용합니다.

## 로컬 서비스 종료

```bash
docker compose down
```

DB 데이터까지 초기화하려면 다음 명령을 사용합니다.

```bash
docker compose down -v
```

`-v` 옵션은 로컬 PostgreSQL 데이터를 삭제하므로 필요한 데이터가 없는지 확인한 뒤 사용하세요.

## 운영 배포

운영 환경에서는 `compose.prod.yml`로 GHCR의 백엔드 이미지와 Caddy를 실행합니다.

```bash
docker compose -f compose.prod.yml pull
docker compose -f compose.prod.yml up -d
```

운영 서버의 `.env`에는 PostgreSQL 접속 정보와 OAuth 설정 외에 AI 서버의 사설 주소 및 내부 API 키를 설정합니다.

```dotenv
DB_URL=jdbc:postgresql://<database-host>:5432/damoyeo
AI_BASE_URL=http://<ai-private-ip>:8000
INTERNAL_API_KEY=<shared-secret>
```

Caddy는 외부의 80·443 포트를 받고 Docker 네트워크의 `backend:8080`으로 요청을 전달합니다. 같은 VPC의 AI 인스턴스가 백엔드 사설 IP의 8080 포트로 직접 접근할 수 있도록 운영 Compose는 호스트의 8080 포트도 공개합니다.

백엔드 EC2의 Security Group은 TCP 8080 인바운드 소스를 AI 인스턴스의 Security Group으로만 제한해야 합니다. AI 서버 포트도 반대로 백엔드 EC2의 Security Group에서 오는 요청만 허용합니다.

운영 컨테이너를 종료하려면 다음 명령을 사용합니다.

```bash
docker compose -f compose.prod.yml down
```

## 환경변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/damoyeo` | 애플리케이션 DB URL |
| `DB_USERNAME` | `damoyeo` | 애플리케이션 DB 사용자 |
| `DB_PASSWORD` | `damoyeo` | 애플리케이션 DB 비밀번호 |
| `POSTGRES_DB` | `damoyeo` | Docker PostgreSQL DB 이름 |
| `POSTGRES_USER` | `damoyeo` | Docker PostgreSQL 사용자 |
| `POSTGRES_PASSWORD` | `damoyeo` | Docker PostgreSQL 비밀번호 |
| `POSTGRES_PORT` | `5432` | 호스트에 공개할 PostgreSQL 포트 |
| `BACKEND_IMAGE` | `ghcr.io/damoyeo-team20/damoyeo-be:latest` | 로컬 Compose에서 사용할 백엔드 이미지 |
| `BACKEND_PORT` | `8080` | 호스트에 공개할 백엔드 포트 |
| `AI_PORT` | `8000` | 호스트에서 실행할 FastAPI 포트 |
| `INTERNAL_API_KEY` | `local-ai-key` | 백엔드–AI 내부 API 공유 키 |
| `GOOGLE_CLIENT_ID` | 없음 | Google OAuth 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | 없음 | Google OAuth 클라이언트 보안 비밀 |
| `GOOGLE_REDIRECT_URI` | 없음 | Google Cloud에 등록한 전체 리디렉션 URI |

개인 설정은 `.env`에 작성하며 저장소에 커밋하지 않습니다.

## Google OAuth 연동

Google OAuth 로그인은 Spring Security OAuth2 Client와 서버 세션 방식으로 동작합니다. 설정은 저장소 루트의 `.env`에서 자동으로 읽으며, 로그인 API와 프런트엔드 연동 방법은 [Google OAuth 가이드](docs/google-oauth.md)를 참고하세요.

## 기본 구조

```text
src
├── main
│   ├── java/com/damoyeo
│   └── resources/application.yml
└── test
    ├── java/com/damoyeo
    └── resources/application.yml
```

도메인 구현을 시작할 때는 `user`, `preference`, `room`, `place`, `revision`, `agent`, `orchestration` 단위의 feature 중심 패키지로 확장할 예정입니다.

DB 스키마 변경은 Hibernate 자동 생성을 사용하지 않고 Flyway migration으로 관리합니다.

## 현재 구현된 API

인증 API를 제외한 모든 API는 Google 로그인이 필요합니다. 로그인 성공 시 Google `sub`를 기준으로 사용자를 생성하거나 프로필을 갱신하고, 서버 세션으로 현재 사용자를 식별합니다.

```text
POST /api/groups
GET  /api/groups
GET  /api/groups/{groupId}
POST /api/groups/join

GET  /api/users/me
POST /api/users/me/onboarding/complete

POST /api/groups/{groupId}/meetings
GET  /api/meetings/{meetingId}
PUT  /api/meetings/{meetingId}/conditions
PUT  /api/meetings/{meetingId}/participants
POST /api/meetings/{meetingId}/submit
PUT  /api/meetings/{meetingId}/availability
GET  /api/meetings/{meetingId}/availability/me
GET  /api/meetings/{meetingId}/coordination
POST /api/meetings/{meetingId}/plan
```

현재 일정 제출 규칙은 다음과 같습니다.

- 일정 제출 후 상태는 `COLLECTING_AVAILABILITY`
- 선택된 참여자는 자신의 가능 날짜를 일정 탐색 범위 안에서 제출
- 모든 참여자가 제출해야 다음 상태로 전환
- 선호조사 마감일은 UI에서 날짜로 입력하며 `Asia/Seoul` 기준 해당 날짜가 끝날 때까지 유효
- 전원 제출 후 오늘 또는 미래의 선호조사 마감일이 있으면 `SURVEYING`
- 전원 제출 후 선호조사 마감일이 없거나 이미 지났으면 `READY_TO_PLAN`
- 사용자가 AI 채팅에서 조율을 요청하면 `/plan`을 통해 `PLANNING`으로 전환
- 일정 작성자만 조건, 참여자, 제출, AI 조율 상태를 변경 가능
- 그룹 `HOST`도 자신이 생성하지 않은 일정은 변경할 수 없음
- 향후 AI 채팅과 재생성 요청도 일정 작성자 권한으로 제한 예정
- 같은 그룹에 참여 중인 멤버만 일정 참여자로 선택 가능

사용자는 Google 계정의 고정 식별자인 `google_subject`를 기준으로 저장합니다.
