# Damoyeo Backend

여러 사용자의 선호와 일정을 바탕으로 모임의 시간, 활동, 장소를 조율하는 Damoyeo 서비스의 백엔드입니다.

현재 저장소에는 팀 개발을 시작하기 위한 최소 Spring Boot 기반만 포함되어 있습니다. 도메인 기능, OAuth, 외부 API 및 실제 Agent 연동은 아직 구현하지 않았습니다.

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

- JDK 21
- Docker 및 Docker Compose
- Git

Gradle은 별도로 설치할 필요가 없습니다. 저장소에 포함된 Gradle Wrapper를 사용합니다.

## 시작하기

```bash
git clone git@github.com:damoyeo-team20/damoyeo-BE.git
cd damoyeo-BE
cp .env.example .env
docker compose up -d
./gradlew bootRun
```

애플리케이션이 실행되면 아래 주소에서 상태를 확인할 수 있습니다.

```text
http://localhost:8080/actuator/health
```

## 테스트

```bash
./gradlew test
```

테스트는 별도의 PostgreSQL 없이 H2 인메모리 DB를 사용합니다.

## 로컬 DB 종료

```bash
docker compose down
```

DB 데이터까지 초기화하려면 다음 명령을 사용합니다.

```bash
docker compose down -v
```

`-v` 옵션은 로컬 PostgreSQL 데이터를 삭제하므로 필요한 데이터가 없는지 확인한 뒤 사용하세요.

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

개인 설정은 `.env`에 작성하며 저장소에 커밋하지 않습니다.

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
