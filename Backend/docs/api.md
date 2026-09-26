# 📱 모바일 앱 설문조사 서비스 API 명세서

> ERD 기반 최종 API 설계 문서입니다.

---

## 1. API 기본 규칙

| 항목 | 내용 |
| :--- | :--- |
| **Base URL** | `/api` |
| **통신 방식** | REST API |
| **데이터 형식** | JSON |
| **인증 방식** | JWT (`Authorization: Bearer {accessToken}`) |
| **문자 인코딩** | UTF-8 |
| **성공 응답** | HTTP 200 또는 201 |

---

## 2. 전체 API 목록

| 구분 | Method | Endpoint | 기능 | 인증 |
| :--- | :--- | :--- | :--- | :---: |
| **인증** | `POST` | `/api/auth/signup` | 회원가입 | X |
| **인증** | `POST` | `/api/auth/login` | 로그인 | X |
| **인증** | `POST` | `/api/auth/logout` | 로그아웃 | O |
| **사용자** | `GET` | `/api/users/me` | 내 정보 조회 | O |
| **사용자** | `GET` | `/api/users/me/responses` | 내 설문 참여 내역 | O |
| **사용자** | `GET` | `/api/users/me/points` | 내 포인트 및 내역 | O |
| **설문** | `GET` | `/api/surveys` | 설문 목록 조회 | X |
| **설문** | `GET` | `/api/surveys/:surveyId` | 설문 상세 조회 | X |
| **설문** | `POST` | `/api/surveys` | 설문 생성 | O |
| **설문** | `PATCH` | `/api/surveys/:surveyId` | 설문 수정 | O |
| **설문** | `DELETE` | `/api/surveys/:surveyId` | 설문 삭제 | O |
| **질문** | `POST` | `/api/surveys/:surveyId/questions` | 질문 추가 | O |
| **질문** | `PATCH` | `/api/questions/:questionId` | 질문 수정 | O |
| **질문** | `DELETE` | `/api/questions/:questionId` | 질문 삭제 | O |
| **참여** | `POST` | `/api/surveys/:surveyId/responses` | 설문 참여 및 답변 제출 | O |
| **결과** | `GET` | `/api/surveys/:surveyId/results` | 설문 결과 및 통계 | O |

---

## 3. 인증 API

### 3-1. 회원가입
* **Method**: `POST`
* **URL**: `/api/auth/signup`
* **인증**: 필요 없음
* **설명**: 새로운 사용자를 생성합니다. (USER 테이블에 사용자를 생성하고 초기 point는 0으로 설정)

**Request Body 예시:**
```json
{
  "nickname": "지호",
  "email": "jiho@example.com",
  "password": "password123",
  "age_group": "20대",
  "region": "서울"
}
```

### 3-2. 로그인
* **Method**: `POST`
* **URL**: `/api/auth/login`
* **인증**: 필요 없음
* **설명**: 이메일과 비밀번호를 확인하고 JWT를 발급합니다.

**Response 예시:**
```json
{
  "accessToken": "JWT_TOKEN",
  "user": {
    "user_id": 1,
    "nickname": "지호",
    "point": 500
  }
}
```

### 3-3. 로그아웃
* **Method**: `POST`
* **URL**: `/api/auth/logout`
* **인증**: 필요 (`Bearer Token`)
* **설명**: 모바일 앱의 로그인 상태를 종료합니다. JWT를 사용하는 경우 서버 측 토큰 정책에 따라 처리합니다.

---

## 4. 사용자 API

### 4-1. 내 정보 조회
* **Method**: `GET`
* **URL**: `/api/users/me`
* **인증**: 필요
* **관련 테이블**: `USER`
* **설명**: 현재 로그인한 사용자의 기본 정보를 조회합니다.

**Response 예시:**
```json
{
  "user_id": 1,
  "nickname": "지호",
  "age_group": "20대",
  "region": "서울",
  "point": 500
}
```

### 4-2. 내 설문 참여 내역
* **Method**: `GET`
* **URL**: `/api/users/me/responses`
* **인증**: 필요
* **관련 테이블**: `USER` → `RESPONSE` → `SURVEY`
* **설명**: 현재 사용자가 참여한 설문 목록을 조회합니다.

### 4-3. 내 포인트 및 내역
* **Method**: `GET`
* **URL**: `/api/users/me/points`
* **인증**: 필요
* **관련 테이블**: `USER` → `POINT_HISTORY`
* **설명**: 현재 포인트와 포인트 변동 내역을 조회합니다.

---

## 5. 설문 API

### 5-1. 설문 목록 조회
* **Method**: `GET`
* **URL**: `/api/surveys`
* **인증**: 필요 없음
* **관련 테이블**: `SURVEY`
* **Query 예시**: `?category=대학생활&status=OPEN`
* **설명**: 모바일 홈 화면에서 사용할 설문 목록을 조회합니다. (응답에는 `survey_id`, `title`, `category`, `reward_point`, `target_count`, `current_count`, `end_date`, `status` 등 포함)

### 5-2. 설문 상세 조회
* **Method**: `GET`
* **URL**: `/api/surveys/:surveyId`
* **인증**: 필요 없음
* **관련 테이블**: `SURVEY` → `QUESTION` → `SURVEY_OPTION`
* **설명**: 앱의 설문 참여 화면을 구성하기 위해 설문, 질문, 선택지를 한 번에 조회합니다.

**Response 예시:**
```json
{
  "survey_id": 1,
  "title": "대학생 생활 만족도",
  "reward_point": 100,
  "questions": [
    {
      "question_id": 10,
      "question": "현재 거주 지역은?",
      "question_type": "single",
      "options": [
        { "option_id": 1, "option_text": "서울" },
        { "option_id": 2, "option_text": "경기" }
      ]
    }
  ]
}
```

### 5-3. 설문 생성
* **Method**: `POST`
* **URL**: `/api/surveys`
* **인증**: 필요
* **관련 테이블**: `SURVEY` → `QUESTION` → `SURVEY_OPTION`
* **설명**: 로그인한 사용자가 새로운 설문을 생성합니다. (설문 정보와 질문/선택지를 함께 받아 저장)

### 5-4. 설문 수정
* **Method**: `PATCH`
* **URL**: `/api/surveys/:surveyId`
* **인증**: 필요
* **설명**: 설문 작성자가 자신의 설문 정보를 수정합니다.

### 5-5. 설문 삭제
* **Method**: `DELETE`
* **URL**: `/api/surveys/:surveyId`
* **인증**: 필요
* **설명**: 설문 작성자가 자신의 설문을 삭제합니다.

---

## 6. 질문 API

| Method | Endpoint | 관련 테이블 | 설명 |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/surveys/:surveyId/questions` | `QUESTION`, `SURVEY_OPTION` | 설문에 질문을 추가 |
| `PATCH` | `/api/questions/:questionId` | `QUESTION`, `SURVEY_OPTION` | 질문 및 선택지 수정 |
| `DELETE` | `/api/questions/:questionId` | `QUESTION`, `SURVEY_OPTION` | 질문 및 관련 선택지 삭제 |

---

## 7. 설문 참여 API

### 7-1. 설문 참여 및 답변 제출
* **Method**: `POST`
* **URL**: `/api/surveys/:surveyId/responses`
* **인증**: 필요
* **관련 테이블**: `RESPONSE` → `ANSWER` → `QUESTION`
* **추가 처리**: `USER.point` 증가 + `POINT_HISTORY` 기록

**Request Body 예시:**
```json
{
  "answers": [
    { "question_id": 10, "answer": "서울" },
    { "question_id": 11, "answer": "매우 만족" }
  ]
}
```

**서버 처리 순서 (단일 트랜잭션):**
1. 로그인한 사용자 확인
2. 설문 존재 여부 확인
3. 설문 상태 및 참여 기간 확인
4. 이미 참여했는지 확인 (`UNIQUE(user_id, survey_id)` 제약조건으로 중복 참여 방지)
5. `RESPONSE` 생성
6. `ANSWER` 생성
7. `USER.point`에 보상 포인트 반영
8. `POINT_HISTORY`에 포인트 지급 내역 기록

---

## 8. 설문 결과 API

### 8-1. 설문 결과 및 통계
* **Method**: `GET`
* **URL**: `/api/surveys/:surveyId/results`
* **인증**: 필요
* **관련 테이블**: `SURVEY` → `QUESTION` → `ANSWER` / `SURVEY_OPTION`
* **설명**: 모바일 앱에서 통계와 그래프를 표시할 수 있도록 응답 수와 선택지별 집계 결과를 제공합니다.

**Response 예시:**
```json
{
  "survey_id": 1,
  "total_responses": 100,
  "questions": [
    {
      "question_id": 10,
      "results": [
        { "option": "서울", "count": 60, "percentage": 60 },
        { "option": "경기", "count": 30, "percentage": 30 }
      ]
    }
  ]
}
```

---

## 9. HTTP 상태 코드

| 코드 | 의미 | 예시 |
| :---: | :--- | :--- |
| **200** | OK | 조회/수정 성공 |
| **201** | Created | 회원가입/설문 생성 성공 |
| **400** | Bad Request | 필수값 누락 또는 잘못된 요청 |
| **401** | Unauthorized | 로그인 또는 JWT 인증 실패 |
| **403** | Forbidden | 다른 사용자의 설문 수정/삭제 시도 |
| **404** | Not Found | 존재하지 않는 사용자/설문/질문 |
| **409** | Conflict | 이미 참여한 설문 |
| **500** | Internal Server Error | 서버 내부 오류 |

---

## 10. ERD와 API 연결 관계

| API | 주요 DB 테이블 | 역할 |
| :--- | :--- | :--- |
| `/auth/signup` | `USER` | 사용자 생성 |
| `/auth/login` | `USER` | 사용자 인증 |
| `/surveys` | `SURVEY` | 설문 조회/생성/수정/삭제 |
| `/surveys/:id` | `SURVEY`, `QUESTION`, `SURVEY_OPTION` | 설문 상세 제공 |
| `/surveys/:id/responses` | `RESPONSE`, `ANSWER`, `USER`, `POINT_HISTORY` | 설문 참여와 보상 처리 |
| `/surveys/:id/results` | `RESPONSE`, `ANSWER`, `QUESTION`, `SURVEY_OPTION` | 설문 통계 계산 |
| `/users/me/points` | `USER`, `POINT_HISTORY` | 포인트 조회 |

---

## 11. 모바일 앱 화면과 API 연결

| 앱 화면 | 사용 API |
| :--- | :--- |
| 회원가입 | `POST /api/auth/signup` |
| 로그인 | `POST /api/auth/login` |
| 홈 / 설문 목록 | `GET /api/surveys` |
| 설문 상세 | `GET /api/surveys/:surveyId` |
| 설문 참여 | `POST /api/surveys/:surveyId/responses` |
| 참여 완료 / 포인트 | `GET /api/users/me/points` |
| 마이페이지 | `GET /api/users/me` |
| 참여 내역 | `GET /api/users/me/responses` |
| 설문 결과 | `GET /api/surveys/:surveyId/results` |