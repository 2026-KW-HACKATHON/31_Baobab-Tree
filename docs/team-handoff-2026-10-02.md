# 10/02 작업 공유 — 프론트·백엔드 연동

프론트 겸 PM인 mok이 화면 연결과 함께 백엔드 계약 정리·인증·응답 저장까지 진행했습니다. 다른 세 명은 아래 기준을 확인한 뒤 기존 담당 작업을 이어가면 됩니다.

**작업 브랜치: `mok` / 커밋: `0a022fc`**  
`feat: connect Android survey flows to schema-backed API`  
팀 공유 기준은 `mok` 브랜치입니다. 예시 DB는 Git에 포함되지 않으므로 각자 생성해야 합니다.

## 오늘 완료한 내용

- **화면 연결:** MainActivity의 화면 전환과 callback 정리, 작성 단계 사이 입력 유지, 선택한 설문을 상세 화면에 전달.
- **조회:** 홈·검색·상세를 실제 서버 데이터에 연결. 로딩·빈 목록·오류·재시도 처리.
- **인증·등록:** 회원가입·로그인, JWT를 포함한 설문 등록. 서버 저장 성공 후에만 완료 화면으로 이동하고 목록 갱신.
- **참여:** 객관식·주관식 입력 → 응답 검증 → 서버 저장 → 완료 화면. 지급·보유 포인트 표시, 참여 인원 갱신, 실패 시 답변 유지.
- **중복 참여:** ‘참여하기’를 누를 때 로그인 계정의 참여 이력을 먼저 확인하고 안내창 표시. 최종 제출에서도 서버가 중복을 차단.
- **백엔드:** 루트 스키마 기준으로 API 필드 통일, 비밀번호 해시·JWT 인증, 응답 저장·인원 증가·포인트 지급을 하나의 트랜잭션으로 처리.
- **예시:** 생활·편의 / 지역·사회 / 교육·학습 / 문화·스포츠 / 경제·상권 / 건강·의료에 각 2개, 총 12개. 각 설문은 객관식 1개 + 주관식 1개.

## 세 팀원이 꼭 알아야 할 기준

| 담당 | 확인하고 이어갈 사항 |
| --- | --- |
| 백엔드 2명 공통 | 실제 구현은 **루트 `src/`**, 기준 스키마는 **루트 `prisma/schema.prisma`**. `Backend/src/`는 루트 구현을 참조하는 진입점이므로 그곳에 별도 구현을 추가하지 말 것. 스키마 변경 시 `Backend/prisma/schema.prisma`도 동일하게 맞출 것. |
| 인증·사용자 관련 작업자 | 사용자 필드는 `loginId`, `name`, `email`, `password`. 보호 API는 `Authorization: Bearer <accessToken>` 사용. 사용자 식별은 요청 body가 아닌 JWT의 `userId` 기준. 기존 `nickname` 기반 DB는 데이터 이전 필요. |
| 설문·응답 관련 작업자 | 질문은 `question`, `questionType: single/short`, 선택지는 `options[].optionText`. 응답은 `answers: [{questionId, answer}]`이며 객관식 `answer`는 **선택지 텍스트**. 필수 질문에 정확히 한 번 답해야 하며 선택 질문은 생략 가능. 중복 참여·마감·정원 초과 차단과 포인트 트랜잭션 유지. |
| 프론트 1명 | 실제 Android 프로젝트는 **`Frontend/BAOBAB_UI`**. 루트 `BAOBAB_UI/*.kt`는 앱 빌드 대상이 아님. 디자인 수정 시 아래 ViewModel 상태와 callback 연결을 유지할 것. **MY 화면은 계정 정보·포인트·내 설문·참여 이력·작성자 통계에 연결됨.** |

프론트 주요 연결 위치:

- `MainActivity.kt`: 화면 전환, 로그인 후 원래 작업 복귀, 참여 이력 확인·안내창.
- `BaobabViewModel.kt`: 내비게이션, 설문 작성 상태와 완료 시점의 초안.
- `SurveyDataViewModel.kt` / `SurveyRepository.kt`: 목록·상세 조회, HTTP 요청과 API 필드 변환.
- `AccountViewModel.kt` / `AccountScreen.kt`: 로그인 토큰, 회원가입·로그인, 등록·응답 제출 상태.
- `ParticipationScreen.kt`: 질문별 답변 입력·검증, 답변 상태 보관.

## 10/03 후속 작업 반영

- **MY:** 계정 정보 수정, 포인트, 내 설문 목록·삭제·공유, 참여 이력과 내가 제출한 답변 조회.
- **결과:** 작성자용 객관식 비율·주관식 응답 화면 연결. 선택 질문의 비율은 해당 질문 응답자 기준.
- **저장:** 소개·대상·소요시간·사진·질문별 필수 여부를 스키마 → API → Android에 연결.
- **로그인:** Android Keystore로 토큰을 암호화해 보관하고 재시작 시 검증·복원. 로그아웃·만료 시 삭제. JWT 유효기간은 1일이며 갱신 토큰은 없음.
- **배포:** 실제 HTTPS 주소는 아직 없음. 릴리스 주소 검증과 운영 JWT_SECRET 검증 준비 완료. [배포 설정](deployment.md) 참고.
- **기존 DB:** 서버를 멈추고 루트에서 `npm run db:migrate:survey-fields`, `npm run db:generate` 실행. 자동 백업 후 추가 필드만 이전하며 기존 데이터는 보존.

## 각자 실행하는 방법

저장소 루트에서 실행합니다. `db:push`는 로컬 DB를 변경하므로 기존 데이터가 있다면 스키마와 이전 필요 여부부터 확인하세요.

```powershell
npm ci
npm run db:push
npm run db:seed:examples
npm start
```

예시 생성은 기존 데이터를 삭제하지 않고 같은 예시의 중복 생성을 건너뜁니다. 예시 작성자 계정은 시드 전용이며 로그인용 공유 계정이 아닙니다. 테스트할 계정은 앱에서 회원가입하세요.

Android Studio에서는 `Frontend/BAOBAB_UI`를 열고 Gradle Sync 후 실행합니다. 에뮬레이터 API 주소는 기본 `http://10.0.2.2:5000/api/`. Android 17 이상에서 요청하는 **주변 기기 권한**을 허용해야 로컬 서버에 접속할 수 있습니다. 실제 휴대폰은 `surveyApiBaseUrl`을 개발 PC의 LAN 주소로 변경하고 다시 빌드해야 합니다.

## 확인한 내용과 팀 테스트 순서

Android 빌드·단위 테스트 **17개**, 백엔드 통합 테스트 **11개** 통과. 백엔드 테스트는 별도 임시 DB를 사용합니다. 수정된 앱의 에뮬레이터 설치·실행도 완료했습니다.

1. 회원가입·로그인 → 설문 만들기 → 홈에서 저장된 설문 확인.
2. 다른 테스트 계정으로 로그인 → 설문 참여 → 답변 제출 → 지급·보유 포인트 확인.
3. 같은 설문에서 ‘참여하기’ → 답변 화면 진입 전 중복 참여 안내 확인.
4. 검색 화면에서 카테고리별 예시 2개씩 조회 확인.

검증 명령은 루트에서 `npm test`, Android 프로젝트에서 `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`입니다.

상세 계약은 [API 문서](api.md), 실행 설정은 [백엔드 안내](../Backend/README.md)와 [Android 안내](../Frontend/BAOBAB_UI/README.md)를 참고하세요.
