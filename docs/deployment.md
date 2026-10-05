# Vercel + Neon 배포

배포 대상은 저장소 루트의 Express API입니다. Android Compose 앱은 APK로 따로 배포합니다.
`baobab-api.vercel.app`은 희망 주소이며 Vercel이 실제 할당하기 전에는 확정된 주소가 아닙니다.

## 1. DB 연결

Neon에서 프로젝트를 만들고 pooled 연결 주소를 `DATABASE_URL`, direct 연결 주소를
`DIRECT_URL`에 설정합니다. Supabase PostgreSQL도 사용할 수 있지만 이번 안내는 Neon 기준입니다.
Prisma 5.22를 유지하며 런타임 연결은 pooler, 스키마 변경은 direct 연결을 사용합니다.

로컬에서 설정할 때는 저장소 루트의 `.env.example`을 `.env`로 복사합니다.
Neon의 실제 URL을 넣고 제공된 SSL 설정은 유지하세요. `DATABASE_URL`에는 필요하면
`connection_limit=2&pool_timeout=15&connect_timeout=15`를 추가합니다.
비밀번호·연결 URL·결제 키를 Git이나 채팅에 올리지 않습니다.

빈 PostgreSQL DB의 전체 테이블(결제 주문·쿠폰·설문 보상 포함)을 생성합니다.

```powershell
npm ci
npm run db:migrate:deploy
npm run db:generate
```

`db:migrate:deploy`는 저장된 PostgreSQL 마이그레이션만 적용합니다. 기존 SQLite 전용
`migrate-*.js`는 PostgreSQL에 실행하지 않습니다. 빌드나 HTTP 요청 중에 마이그레이션하지 않습니다.
기존 로컬 데이터를 이전합니다. 로컬 서버를 멈추고, 대상 PostgreSQL에 위 마이그레이션을 적용한 뒤
저장소 루트에서 실행합니다. 대상 테이블은 모두 비어 있어야 합니다.

```powershell
npm run db:import:sqlite
```

스크립트는 SQLite 백업을 만들고 기존 ID·비밀번호 해시·설문·포인트·결제 주문·쿠폰을
PostgreSQL에 하나의 트랜잭션으로 옮깁니다. 테이블별 행 수와 전체 포인트 합계를 확인하고,
자동 증가 ID의 sequence도 갱신합니다. 실패하면 대상 변경을 롤백하며 원본 SQLite는 보존합니다.
결제 주문·쿠폰 테이블이 아직 없는 로컬 DB에서는 그 두 테이블은 빈 상태로 이전합니다.
다른 위치의 원본은 `SQLITE_IMPORT_PATH`로 지정합니다. 기존 세션을 유지하려면
배포 JWT_SECRET도 기존 서버와 동일해야 합니다. 이전 이후에는 클라우드 서버를 기준으로 사용합니다.
예시 설문은 원하는 경우 `npm run db:seed:examples`로 추가합니다.

## 2. PostgreSQL 검증

테스트용 PostgreSQL 주소를 로컬 `.env`의 `TEST_DATABASE_URL`에 설정한 뒤 실행합니다.
테스트는 무작위 이름의 전용 schema를 만들고 그 schema만 삭제합니다.
`public` schema나 기존 앱 데이터를 초기화하지 않습니다.

```powershell
npm test
npm run test:postgres
```

`npm test`는 임시 SQLite DB에서 계약·설문·쿠폰·결제 중복 지급 방지와 배포 설정을 검증합니다.
`test:postgres`는 실제 PostgreSQL에서 같은 API 테스트와 동시 요청 검증을 실행합니다.
결제 제공자 호출은 테스트에서 모의 응답을 사용합니다. 실제 카카오·토스 결제창 검증은 별도입니다.

## 3. Vercel 연결

GitHub 저장소를 Import하고 다음으로 설정합니다.

| 항목 | 값 |
| --- | --- |
| Root Directory | 저장소 루트 `.` (Frontend 또는 Backend 폴더가 아님) |
| Framework | Express |
| Node.js | 22.x |
| Install Command | `npm ci` |
| Build Command | `npm run build:vercel` |
| Production Branch | `mok` |

`vercel.json`이 프레임워크와 빌드 명령을 지정합니다. Express의 `/api/...` 경로는 그대로 유지합니다.
Vercel은 서버 시작 명령 대신 Express export를 사용합니다. 로컬에서는 `npm start`를 사용합니다.
빌드는 Prisma 클라이언트만 생성하고 DB를 변경하지 않습니다.

Vercel 환경변수에 아래 값을 설정합니다. Preview는 별도 DB·테스트 키를 사용하고,
모바일 결제 콜백에 사용되는 Production URL은 로그인 보호 없이 접근 가능해야 합니다.

| 환경변수 | 값 |
| --- | --- |
| `NODE_ENV` | `production` |
| `DATABASE_URL` | Neon pooled URL |
| `DIRECT_URL` | Neon direct URL |
| `JWT_SECRET` | 최소 32자 무작위 비밀값 |
| `PAYMENT_PUBLIC_BASE_URL` | 실제 HTTPS 배포 origin, `/api` 제외 |
| `TOSS_CLIENT_KEY` | API 개별 연동 테스트 키 `test_ck_...` |
| `TOSS_SECRET_KEY` | 같은 상점의 테스트 키 `test_sk_...` |
| `KAKAOPAY_SECRET_KEY` | 카카오 개발용 시크릿 키 |
| `KAKAOPAY_CID` | 개발용 CID |

카카오 설정에도 실제 사용하는 배포 도메인을 등록합니다. 환경변수 변경 후 재배포합니다.
배포 후 `GET https://<실제-주소>/api/health`가
`{"status":"ok","database":"connected"}`를 반환하는지 확인합니다.
Production DB URL이 없으면 서버 시작이 실패하며 SQLite로 대체하지 않습니다.

## 4. Android APK

실제 배포가 검증된 뒤 Android 프로젝트에서 주소를 지정해 빌드합니다.

```powershell
.\gradlew.bat :app:assembleDebug -PsurveyApiBaseUrl=https://<실제-주소>/api/
.\gradlew.bat :app:assembleRelease -PreleaseApiBaseUrl=https://<실제-주소>/api/
```

팀 테스트는 debug APK를 사용할 수 있습니다. 배포용 release APK에는 별도 서명이 필요합니다.
결제 키는 APK에 넣지 않습니다. `10.0.2.2`는 PC 개발 서버용이며 배포된 앱에서는 사용하지 않습니다.

## PC 개발 유지

`DATABASE_URL`을 비워두면 개발 모드에서는 기존 SQLite DB를 사용합니다.
`npm ci` 또는 `npm run db:generate`는 PostgreSQL 클라이언트와 로컬 SQLite 클라이언트를 생성합니다.
새 로컬 DB는 `npm run db:push:local`로 만들 수 있습니다. 기존 DB는 백업 후 해당 SQLite
변경 스크립트를 실행하세요. PostgreSQL 스키마와 SQLite 스키마의 모델은 동일하게 유지합니다.

참고: [Vercel Express](https://vercel.com/docs/frameworks/backend/express),
[Prisma 트랜잭션](https://www.prisma.io/docs/orm/v6/prisma-client/queries/transactions),
[Neon 연결](https://www.prisma.io/docs/orm/v6/overview/databases/neon).
