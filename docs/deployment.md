# 배포 설정

실제 배포 주소는 아직 정해지지 않았습니다. 서버와 Android 빌드가 아래 설정을 요구하도록 준비했습니다.

## 서버

루트 `.env.production.example`을 참고해 배포 환경에서 `NODE_ENV=production`, `PORT`, `JWT_SECRET`을 설정하세요.
JWT_SECRET은 최소 32자 이상의 무작위 비밀값이며 소스나 공유 문서에 넣지 않습니다. 기본 개발 키나 짧은 키로 운영 서버를 시작하면 실패합니다.
운영 플랫폼의 비밀값 설정을 사용하거나 서버의 비공개 `.env` 파일에 저장하세요.
키 생성 예시: `node -e "console.log(require('node:crypto').randomBytes(48).toString('hex'))"`.
키를 바꾸면 기존 로그인 토큰은 무효화됩니다.

서버는 HTTP로 수신합니다. 호스팅 서비스 또는 리버스 프록시에서 인증서를 설정해 외부 `https://<도메인>/api/`를 내부 포트로 전달하세요.
SQLite DB와 백업은 영구 저장소에 보관하세요. 새 DB에는 `npm run db:push`를 사용합니다.
현재 스키마의 기존 DB는 서버를 멈추고 다음 순서로 실행합니다. 더 오래된 nickname 스키마는 별도 이전이 필요합니다.

```powershell
npm run db:migrate:survey-fields
npm run db:generate
npm start
```

필드 이전은 백업을 만든 후 기존 행 수를 확인하며 소개·대상·시간·사진과 필수 여부만 추가합니다.

## Android

Android 프로젝트에서 실제 HTTPS 주소를 지정합니다.

```powershell
.\gradlew.bat :app:assembleRelease -PreleaseApiBaseUrl=https://<실제-도메인>/api/
```

주소가 없거나 HTTP·로컬 개발 주소이면 릴리스 빌드가 실패합니다. 스토어 배포용 서명은 별도 설정해야 합니다.
디버그는 기존 `http://10.0.2.2:5000/api/` 또는 `-PsurveyApiBaseUrl=http://<개발-PC-IP>:5000/api/`를 사용합니다.
PC 브라우저에서 에뮬레이터 주소 대신 `http://localhost:5000/api/surveys`로 조회합니다.

로그인 토큰은 Android Keystore AES-GCM으로 암호화해 보관하며 백업·기기 이전에서 제외합니다.
앱 재시작 시 저장한 토큰으로 사용자 정보를 확인하고 복원합니다. 서버 장애 시 토큰은 보존하고 재시도할 수 있습니다.
401 만료 응답이나 로그아웃 시 토큰을 삭제합니다. JWT는 1일 유효하며 갱신 토큰이 없으므로 만료 후 다시 로그인합니다.
공유 링크 `baobab://surveys/<id>`는 앱 설치와 같은 API 서버 접근이 필요합니다.


리워드 환불 업데이트: 서버를 멈추고 루트에서 npm run db:migrate:reward-refund 실행 후 npm run db:generate 실행. 기존 DB는 자동 백업하며 실제 등록 차감액을 기록한 설문만 환불합니다.
