# Android와 백엔드 연결

홈·검색 목록은 `GET /api/surveys`, 선택한 설문 상세는
`GET /api/surveys/:id`로 조회합니다. 내려받은 목록에서 검색어와 카테고리를
필터링하며, 질문·선택지 ID도 상세 데이터에 보관합니다.
샘플 목록은 Compose Preview와 테스트에서만 사용합니다.

저장소 루트에서 백엔드를 준비하고 실행하세요.

```powershell
npm ci
npm run db:push
npm start
```

기존 DB가 있다면 [백엔드 실행 안내](../../Backend/README.md)의 데이터 이전
사항을 먼저 확인하세요. 설문이 없는 DB에서는 앱에 빈 목록이 표시됩니다.
등록 요청 예시는 [API 문서](../../docs/api.md)에 있습니다.

기본 API 주소는 `http://10.0.2.2:5000/api/`입니다.
Android 에뮬레이터에서 개발 PC의 로컬 서버를 사용하는 설정입니다.
Android 17(API 37) 이상에서는 로컬 서버 접속을 위해 앱이 요청하는
`주변 기기` 권한을 허용하세요. 권한이 없으면 서버 조회 전에 안내 화면을 표시합니다.

실제 기기나 배포 서버를 사용한다면 이 Android 프로젝트의
`gradle.properties`에 다음 속성을 설정하고 앱을 다시 빌드하세요.

```properties
surveyApiBaseUrl=http://192.168.0.10:5000/api/
```

주소는 개발 PC의 실제 LAN IP 또는 배포 서버 주소로 바꿉니다.
실제 기기와 개발 PC는 서버에 접근할 수 있는 네트워크에 있어야 합니다.
디버그 빌드에서만 HTTP 통신을 허용하므로 배포 빌드는 HTTPS 주소를 사용하세요.
명령행에서도 주소를 지정할 수 있습니다.

```powershell
.\gradlew.bat :app:assembleDebug -PsurveyApiBaseUrl=http://192.168.0.10:5000/api/
```

API 요청은 작업 스레드에서 실행합니다. 로딩·빈 목록·오류 상태를 표시하며,
오류 화면의 `다시 시도`로 재요청할 수 있습니다. 목록은 앱 실행 중 ViewModel에
보관하고 상세는 진입할 때마다 조회합니다. 늦게 도착한 이전 상세 응답은
새로 선택한 설문을 덮어쓰지 않습니다.

회원가입 후 로그인하면 설문을 등록할 수 있습니다. Guest는 조회만 가능합니다.
등록 버튼은 JWT를 포함한 `POST /api/surveys`를 호출하며, 서버 성공 응답 후에만
완료 화면으로 이동하고 홈 목록을 새로고침합니다. 실패하면 작성 내용을 유지합니다.
로그인 토큰은 Android Keystore로 암호화해 저장하고 앱 재시작 시 검증·복원합니다.
MY에서 로그아웃할 수 있으며 토큰 만료 시 다시 로그인합니다. 서버 장애 시 저장된 토큰은 보존합니다.
소개·대상·소요시간·질문별 필수 여부·첨부 사진을 서버에 저장합니다.
사진을 첨부하지 않으면 카테고리별 기본 배경을 사용합니다.
설문 참여는 로그인 후 질문별 답변을 입력하고 `POST /api/surveys/:id/responses`로
제출합니다. 필수 질문에 답해야 하며 선택 질문은 생략할 수 있습니다. 객관식은 내려받은 선택지 중 하나를 선택합니다.
성공 응답의 지급·보유 포인트를 완료 화면에 표시하고 목록·상세를 갱신합니다.
실패 시 답변을 유지하며, 로그인 만료 후 재로그인해 이어서 제출할 수 있습니다.
중복 참여·마감·정원 초과는 서버에서 거절합니다. MY 화면에서 계정 정보·보유 포인트·참여 횟수·내가 만든 설문을 조회합니다. 내 설문을 선택하면 작성자 전용 결과 API로 객관식 응답 비율과 주관식 답변을 보여줍니다.
참여하기를 누를 때 `GET /api/users/me/responses`로 로그인 계정의 참여 이력을
조회하고, 이미 참여한 설문은 답변 화면으로 이동하기 전에 안내합니다.
저장소 루트에서 `npm run db:seed:examples`를 실행하면 기존 6개 카테고리별로
2개씩 예시 설문을 추가합니다. 기존 설문을 삭제하거나 같은 예시를 다시 추가하지 않습니다.

검증 명령:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

HTTP 테스트는 JVM의 로컬 테스트 서버를 사용해 스키마 필드 변환,
질문·선택지 ID 보존, 빈 응답, 오류 응답을 확인합니다.
ViewModel 테스트는 재시도와 이전 응답 무시 동작을 확인합니다.

마이페이지의 `계정 정보 > 수정`에서 이름·이메일·지역·연령대를 변경합니다.
`PATCH /api/users/me`는 로그인 토큰으로 본인 계정만 수정하며 아이디·비밀번호·포인트는 변경하지 않습니다.

설문 상세·내 설문 목록·통계 화면의 `공유`는 Android 공유창을 열고
`baobab://surveys/<id>` 링크를 전달합니다. 이 링크를 처리하는 BAOBAB 앱이
설치되어 있어야 하며 수신 기기도 같은 API 서버에 접근할 수 있어야 합니다.
배포 웹사이트를 연결하지 않았으므로 앱이 없는 브라우저에서는 설문이 열리지 않습니다.

MY에서 참여한 설문과 내가 제출한 답변도 조회합니다.
기존 DB는 서버를 멈추고 루트에서 `npm run db:migrate:survey-fields`, `npm run db:generate`를 실행하세요.
이전 과정에서 SQLite 백업을 만들고 기존 데이터를 보존합니다.

릴리스 빌드는 `-PreleaseApiBaseUrl=https://<실제-도메인>/api/`를 반드시 지정해야 합니다.
주소 미설정·HTTP·로컬 개발 주소는 빌드 단계에서 거절합니다.
운영 서버의 JWT_SECRET 설정과 실행 순서는 [배포 안내](../../docs/deployment.md)를 참고하세요.

토스 테스트 결제는 서버 실행 위치의 `.env`에 아래 설정이 필요합니다. 저장소 루트에서
`npm start`를 실행한다면 저장소 루트의 `.env`를 사용합니다. 실제 키는 커밋하지 않습니다.

```dotenv
TOSS_CLIENT_KEY=<API 개별 연동 테스트 클라이언트 키: test_ck_로 시작>
TOSS_SECRET_KEY=<같은 상점의 테스트 시크릿 키: test_sk_로 시작>
PAYMENT_PUBLIC_BASE_URL=<결제 브라우저가 접근할 수 있는 서버 주소, /api 제외>
```

Android 에뮬레이터에서 이 PC의 서버를 이용할 때 연결 주소 예시는
`http://10.0.2.2:5000`입니다. 실기기는 PC의 LAN 주소나 외부에서 접근 가능한 서버 주소를
사용해야 합니다. 키나 주소를 바꾼 후에는 서버를 재시작합니다.
카카오페이는 별도로 `KAKAOPAY_SECRET_KEY`, `KAKAOPAY_CID`를 설정합니다.

기존 DB에 결제·쿠폰 테이블이 없다면 서버를 멈춘 뒤 저장소 루트에서 다음을 실행합니다.
마이그레이션 스크립트는 기존 DB 백업을 만든 다음 테이블을 추가합니다.

```powershell
node scripts/migrate-payment-orders.js
node scripts/migrate-coupons.js
npm run db:generate
npm start
```

결제 API의 설정 오류는 `TOSS_NOT_CONFIGURED`, `KAKAO_NOT_CONFIGURED`,
`PAYMENT_URL_NOT_CONFIGURED` 코드로 구분합니다. 앱에는 설정 안내를 표시하고,
그 밖의 서버 내부 오류는 상세 내용을 노출하지 않습니다.
