# SPRING PLUS

CH 5 플러스 Spring 과제

## 주요 기능

- 회원가입 및 로그인
- JWT 기반 인증
- Spring Security 기반 인가 처리
- 사용자 닉네임 정보 저장 및 JWT claim 포함
- 일정 생성, 단건 조회, 목록 조회
- 날씨 조건 및 수정일 기간 조건으로 일정 목록 검색
- QueryDSL 기반 일정 검색
- 댓글 등록 및 조회
- 담당자 등록, 조회, 삭제
- 관리자 권한으로 사용자 역할 변경
- AOP 기반 관리자 접근 로그 기록

## 인증 및 인가

인증은 JWT를 사용합니다.

- `/auth/**` 요청은 인증 없이 접근할 수 있습니다.
- `/admin/**` 요청은 `ADMIN` 권한이 필요합니다.
- 그 외 요청은 인증된 사용자만 접근할 수 있습니다.

JWT 인증 성공 시 `AuthUser`를 Spring Security의 `SecurityContext`에 저장하고, 컨트롤러에서는 `@AuthenticationPrincipal`로 인증 사용자 정보를 사용합니다.



## 주요 API

### Auth

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/auth/signup` | 회원가입 |
| POST | `/auth/signin` | 로그인 |

### Todo

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/todos` | 일정 생성 |
| GET | `/todos` | 일정 목록 조회 |
| GET | `/todos/{todoId}` | 일정 단건 조회 |
| GET | `/todos/search` | QueryDSL 기반 일정 검색 |

`/todos` 목록 조회는 `weather`, `startDate`, `endDate` 조건을 선택적으로 사용할 수 있습니다.

```http
GET /todos?weather=Rainy&startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
```

`/todos/search` 검색 API는 제목, 생성일 범위, 담당자 닉네임 조건을 선택적으로 사용할 수 있습니다.

```http
GET /todos/search?title=공부&managerNickname=다빈&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59&page=1&size=10
```

검색 결과는 일정 전체 정보가 아니라 다음 정보만 반환합니다.

- 일정 제목
- 담당자 수
- 댓글 수

### Comment

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/todos/{todoId}/comments` | 댓글 등록 |
| GET | `/todos/{todoId}/comments` | 댓글 목록 조회 |

댓글 목록 조회 시 작성자 정보를 fetch join으로 함께 조회해 N+1 문제를 방지했습니다.

### Manager

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/todos/{todoId}/managers` | 담당자 등록 |
| GET | `/todos/{todoId}/managers` | 담당자 목록 조회 |
| DELETE | `/todos/{todoId}/managers/{managerId}` | 담당자 삭제 |

일정 생성 시 생성자는 담당자로 자동 등록됩니다. 이때 JPA cascade를 사용해 `Todo` 저장 시 `Manager`도 함께 저장되도록 처리했습니다.

### Admin

| Method | URL | 설명 |
| --- | --- | --- |
| PATCH | `/admin/users/{userId}` | 사용자 권한 변경 |

관리자 권한 변경 API는 Spring Security의 `hasRole("ADMIN")`으로 접근 권한을 제어합니다.

## QueryDSL 검색 기능

`/todos/search` API는 QueryDSL과 Projection을 사용해 필요한 필드만 조회합니다.

- 제목 부분 일치 검색
- 생성일 범위 검색
- 담당자 닉네임 부분 일치 검색
- 생성일 최신순 정렬
- 페이징 처리
- 담당자 수, 댓글 수 집계

담당자 닉네임 검색은 `exists` 서브쿼리로 분리하여, 닉네임 조건이 있더라도 담당자 수는 해당 일정의 전체 담당자 수로 계산되도록 처리했습니다.