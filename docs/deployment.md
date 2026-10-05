# BAOBAB 사용·배포 안내 — Vercel + Neon

## 현재 상태와 읽는 순서

2026-10-05 기준, `mok` 브랜치에 Vercel 배포 설정·PostgreSQL 마이그레이션·기존 SQLite 데이터 이전 도구가 올라가 있습니다.
**실제 Neon DB 생성, 데이터 이전, Vercel 배포, 배포 주소가 적용된 APK 제작은 아직 완료되지 않았습니다.**
`https://baobab-api.vercel.app`은 희망 주소입니다. 이후 안내의 `<실제-주소>`에는 Vercel에서 확인한 Production 도메인을 넣습니다.

| 맡은 일 | 읽을 부분 |
| --- | --- |
| 처음 서버를 배포하는 사람 | 1 → 2 → 3 → 4 순서 |
| Android APK를 만드는 사람 | 서버 배포 확인 후 5 |
| APK를 받아 테스트하는 팀원 | 6 |
| PC에서 코드를 수정하는 사람 | 7 |
| 문제가 생긴 사람 | 8 |

구성은 `Android APK → Vercel Express API → Neon PostgreSQL`입니다.
배포 후에는 서버 담당자 PC가 꺼져 있어도 앱이 Vercel 서버에 연결합니다.

## 1. 작업 위치·브랜치·환경 설정

서버 명령은 **저장소 최상위 `31_Baobab-Tree`**에서 실행합니다. `package.json`, `src`, `prisma`가 함께 있는 폴더입니다.
Android 명령은 **`Frontend/BAOBAB_UI`**에서 실행합니다. 두 위치를 혼동하지 마세요.

```powershell
# 31_Baobab-Tree 폴더에서
git switch mok
git pull --ff-only origin mok
```

Node.js 22.x와 npm을 사용합니다. 로컬 서버가 실행 중이라면 해당 터미널에서 `Ctrl+C`로 멈춥니다.
Windows에서는 실행 중인 서버가 Prisma 엔진을 사용하고 있으면 `npm ci`나 클라이언트 생성에서 `EPERM`이 날 수 있습니다.

루트 `.env.example`은 항목을 알려주는 템플릿이고, `.env`는 실제 값을 넣는 개인 파일입니다.
`.env`는 Git에서 제외되므로 clone/pull로 전달되지 않습니다. 이미 `.env`가 있으면 기존 결제 키를 유지하며 필요한 항목만 추가합니다.
파일이 없는 경우에만 복사하세요.

```powershell
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item -LiteralPath .env.example -Destination .env
}
```

로컬 `.env`와 Vercel 환경변수는 별개입니다. PC에 설정해도 Vercel에 자동으로 전달되지 않습니다.

## 2. Neon DB 만들기와 기존 데이터 이전

### 2-1. 연결 주소 설정

Neon에 로그인해 BAOBAB용 프로젝트와 DB를 생성합니다. 프로젝트의 **Connect** 화면에서 같은 DB·사용자의 연결 주소 두 개를 복사합니다.

| Neon 연결 주소 | 루트 `.env` 항목 | 용도 |
| --- | --- | --- |
| Connection pooling 켜기, 호스트에 `-pooler` 포함 | `DATABASE_URL` | 서버의 평상시 조회·저장 |
| Connection pooling 끄기 | `DIRECT_URL` | 테이블 생성·기존 데이터 이전 |

Neon이 제공한 실제 연결 URL과 SSL 옵션을 유지하세요. [Neon 연결 방식](https://neon.com/blog/postgres-support-case-recap)
현재 프로젝트는 Prisma 5.22를 사용합니다. 필요하면 pooled URL의 기존 쿼리 뒤에 `&connection_limit=2&pool_timeout=15&connect_timeout=15`를 추가할 수 있습니다.

루트 `.env`의 예시입니다. 아래 자리표시자를 실제 값으로 교체합니다.

```dotenv
NODE_ENV=development
PORT=5000
DATABASE_URL="<Neon pooled 연결 URL>"
DIRECT_URL="<같은 Neon DB의 direct 연결 URL>"
JWT_SECRET="<최소 32자 무작위 비밀값>"
```

JWT 비밀값은 다음으로 생성할 수 있습니다. 생성한 값은 개인 `.env`와 Vercel에만 저장합니다.

```powershell
node -e "console.log(require('node:crypto').randomBytes(48).toString('hex'))"
```

기존 서버와 다른 JWT 비밀값을 사용하면 앱에서 다시 로그인해야 합니다. 계정의 비밀번호 해시는 데이터 이전 시 유지됩니다.

### 2-2. 빈 PostgreSQL에 테이블 생성

루트에서 실행합니다. 서버는 멈춘 상태로 진행합니다.

```powershell
npm ci
npm run db:validate
npm run db:migrate:deploy
```

마이그레이션에 계정·설문·응답·포인트·결제 주문·쿠폰 테이블이 포함돼 있습니다.
SQLite용 `migrate-survey-fields.js`, `migrate-reward-refund.js`, `migrate-payment-orders.js`, `migrate-coupons.js`는 Neon에 실행하지 않습니다.
Vercel 빌드도 마이그레이션을 대신 실행하지 않습니다.

### 2-3. 기존 SQLite 데이터 이전

**실제 기존 데이터가 있는 PC에서 한 사람이 실행**합니다. 원본은 기본적으로 루트 `prisma/dev.db`입니다.
클론한 저장소에는 `.db` 파일이 없으므로 새로 클론한 PC에서 기존 데이터가 자동으로 나타나지 않습니다.
이전이 끝나기 전에는 클라우드 DB에 회원가입하거나 예시 데이터를 추가하지 않습니다. 대상 테이블이 모두 비어 있어야 합니다.

```powershell
npm run db:import:sqlite
```

스크립트는 원본 옆에 `dev.before-postgres-import-<시간>.db` 백업을 만든 뒤,
기존 ID·계정·비밀번호 해시·설문·답변·포인트 내역·결제 주문·쿠폰을 한 트랜잭션으로 이전합니다.
테이블별 행 수와 전체 포인트 합계를 확인하고, 이후 새 행의 ID가 겹치지 않도록 sequence를 갱신합니다.
결제 주문·쿠폰 테이블이 아직 없는 원본에서는 그 두 테이블을 빈 상태로 이전합니다.
다른 경로의 원본은 로컬 `.env`의 `SQLITE_IMPORT_PATH`로 지정합니다.

성공 시 `counts`와 `totalPoints`가 출력됩니다. 실패하면 대상 변경을 롤백하며 원본 파일은 보존합니다.
성공한 대상 DB에 반복 실행하면 빈 DB 조건에서 거절됩니다. 재시도하려고 DB를 임의로 삭제하지 마세요.
이전 이후에는 클라우드 DB를 기준으로 사용하며, 원본 SQLite와 클라우드 데이터를 번갈아 수정하지 않습니다.

### 2-4. 검증

```powershell
npm test
```

실제 PostgreSQL 테스트는 별도 테스트 DB/브랜치의 **direct 연결 URL**을 `.env`의 `TEST_DATABASE_URL`에 설정한 뒤 실행합니다.
이 테스트는 무작위 전용 schema를 만들고 그 schema만 제거합니다. 기존 `public` 데이터는 초기화하지 않습니다.

```powershell
npm run test:postgres
```

2026-10-05 코드 전환 검증: 로컬 테스트 27개 통과, PostgreSQL 연결이 필요한 2개는 아직 미실행입니다.
결제 제공자 호출은 테스트에서 모의 응답을 사용하므로 실제 결제창 확인은 6번에서 진행합니다.

## 3. Vercel 프로젝트 만들기

Vercel에서 GitHub 계정을 연결하고 **Add New → Project**로 이 저장소를 Import합니다.

| 설정 | 값 |
| --- | --- |
| GitHub Repository | `2026-KW-HACKATHON/31_Baobab-Tree` |
| Root Directory | 저장소 루트 `.` |
| Framework Preset | Express |
| Node.js | 22.x |
| Install Command | `npm ci` |
| Build Command | `npm run build:vercel` |
| Production Branch | `mok` |

`Frontend/BAOBAB_UI`나 `Backend`를 Root Directory로 선택하지 않습니다.
Production Branch가 기본 `main`으로 되어 있으면 `mok`으로 변경하고 해당 브랜치로 배포합니다.
프로젝트 설정 위치는 [Vercel 설정 안내](https://vercel.com/docs/project-configuration/project-settings)를 참고하세요.

`vercel.json`이 Express 설정과 빌드 명령을 제공합니다. Vercel은 Express export를 실행하므로 Start Command로 `npm start`를 넣을 필요가 없습니다.
빌드는 Prisma 클라이언트 생성까지만 수행합니다.

## 4. Vercel 환경변수·결제 주소·배포 확인

프로젝트의 **Settings → Environment Variables**에 아래 항목을 **Production** 대상으로 추가합니다.
루트 `.env`의 실제 값을 복사하되, `NODE_ENV`와 콜백 주소는 배포용 값으로 변경합니다.

| 환경변수 | 배포 서버의 값 |
| --- | --- |
| `NODE_ENV` | `production` |
| `DATABASE_URL` | 데이터 이전한 Neon DB의 pooled URL |
| `DIRECT_URL` | 같은 DB의 direct URL |
| `JWT_SECRET` | 최소 32자 무작위 비밀값 |
| `PAYMENT_PUBLIC_BASE_URL` | `https://<실제-주소>` — `/api` 제외 |
| `TOSS_CLIENT_KEY` | API 개별 연동 테스트 키 `test_ck_...` |
| `TOSS_SECRET_KEY` | 같은 상점의 테스트 키 `test_sk_...` |
| `KAKAOPAY_SECRET_KEY` | 카카오 개발용 시크릿 키 |
| `KAKAOPAY_CID` | 카카오 개발용 CID |

토스 위젯용 키가 아니라 현재 코드에 맞는 **API 개별 연동 테스트 키 한 쌍**을 사용합니다.
카카오의 해당 키를 사용하는 앱 설정에도 실제 배포 도메인을 등록합니다.
`TEST_DATABASE_URL`, `SQLITE_IMPORT_PATH`는 Vercel 서버 운영에 필요하지 않습니다.

환경변수 저장 후 배포합니다. 최초 배포 후 실제 도메인을 확인해 `PAYMENT_PUBLIC_BASE_URL`을 맞추고 **Redeploy**합니다.
환경변수 수정은 이미 실행 중인 배포에 자동 반영되지 않고 다음 배포부터 적용됩니다. [Vercel 환경변수 안내](https://vercel.com/docs/environment-variables)
Preview를 사용한다면 DB·환경변수를 별도로 구성합니다.

로그아웃한 브라우저나 휴대폰에서 다음 주소를 엽니다.

```text
https://<실제-주소>/api/health
```

정상 응답은 아래와 같습니다.

```json
{"status":"ok","database":"connected"}
```

Vercel 로그인 화면이 나온다면 앱도 API에 접근할 수 없습니다.
Production 도메인의 공개 API·결제 콜백에 인증 보호가 걸려 있는지 **Settings → Deployment Protection**에서 확인합니다.
[Vercel 배포 보호 안내](https://vercel.com/docs/deployment-protection)
그다음 `/api/surveys`에서 이전한 설문 목록이 조회되는지 확인합니다.

## 5. 배포 서버에 연결되는 APK 만들기

서버 주소를 확인한 뒤, 저장소 루트에서 Android 폴더로 이동합니다.

```powershell
Set-Location Frontend/BAOBAB_UI
.\gradlew.bat :app:assembleDebug -PsurveyApiBaseUrl=https://<실제-주소>/api/
```

생성된 팀 테스트용 APK는 **`app/build/outputs/apk/debug/app-debug.apk`**입니다.
이 APK를 팀원 휴대폰에 설치합니다. 에뮬레이터에서도 같은 클라우드 서버 주소를 사용할 수 있습니다.
이미 설치된 다른 서명의 앱과 충돌하면 설치가 실패할 수 있습니다. 기존 앱을 삭제하면 로컬 로그인·작성 중인 데이터도 지워질 수 있으니 설치 오류를 먼저 확인하세요.

배포용 release 빌드 명령은 다음과 같으며, 별도 서명 설정이 필요합니다.

```powershell
.\gradlew.bat :app:assembleRelease -PreleaseApiBaseUrl=https://<실제-주소>/api/
```

주소 예시의 차이를 확인하세요. 실제 도메인이 예시와 같을 때만 그대로 사용합니다.

| 항목 | 예시 |
| --- | --- |
| Android API 주소 | `https://baobab-api.vercel.app/api/` |
| 결제 콜백 기준 주소 | `https://baobab-api.vercel.app` |
| 브라우저 서버 상태 확인 | `https://baobab-api.vercel.app/api/health` |

## 6. APK를 받아 사용하는 팀원

**배포 서버 주소가 반영된 APK를 받았다면 Node.js·Neon·`.env`·로컬 서버 설정은 필요 없습니다.**
인터넷 연결 후 APK를 설치하고 앱에서 회원가입 또는 기존 계정으로 로그인하면 됩니다.
설문이 빈 목록이거나 서버 오류가 나오면 서버 담당자에게 문의합니다.

팀 테스트 순서:

1. 기존 계정 로그인 → MY에서 이름·잔액·쿠폰과 참여 이력 확인.
2. 홈에서 기존 설문 조회 → 다른 계정으로 참여 → 포인트와 참여 인원 확인.
3. 포인트 충전 → 토스 또는 카카오 선택 → 브라우저 결제창 열기 → 테스트 결제 완료.
4. 앱으로 돌아와 잔액 새로고침 → 충전 포인트 확인.
5. 쿠폰 교환 → 포인트 차감·쿠폰함 반영 확인.

오류 보고 시 **어느 단계에서 멈췄는지**, 앱 오류 문구, 브라우저 페이지/결제창 상태, 발생 시각을 보냅니다.
결제 키·DB 비밀번호·로그인 토큰·콜백 URL의 `state` 값은 가립니다.

## 7. PC에서 로컬 개발하기

### 기존 SQLite로 개발

루트 `.env`에서 `NODE_ENV=development`, `DATABASE_URL`과 `DIRECT_URL`을 빈 값으로 둡니다.
새 PC에서 새 로컬 DB를 만들 때만 아래 순서로 실행합니다.

```powershell
npm ci
npm run db:validate:local
npm run db:push:local
npm start
```

기존 `prisma/dev.db`가 있으면 먼저 백업과 필요한 SQLite 변경 스크립트를 확인합니다.
필요한 경우 서버를 멈추고 `npm run db:migrate:survey-fields`, `npm run db:migrate:reward-refund`,
`node scripts/migrate-payment-orders.js`, `node scripts/migrate-coupons.js`를 적용한 뒤 `npm run db:generate`를 실행합니다.
과거 `nickname` 스키마는 사용자 필드의 별도 이전이 필요합니다.

| 실행 위치 | 앱 API 주소 | `PAYMENT_PUBLIC_BASE_URL` |
| --- | --- | --- |
| Android 에뮬레이터 + PC 서버 | `http://10.0.2.2:5000/api/` | `http://10.0.2.2:5000` |
| 실제 휴대폰 + PC 서버 | `http://<PC의 LAN IP>:5000/api/` | `http://<PC의 LAN IP>:5000` |
| 배포 서버 | `https://<실제-주소>/api/` | `https://<실제-주소>` |

로컬 결제도 `.env`에 토스·카카오 테스트 설정이 필요합니다. 휴대폰과 PC는 서버에 접근 가능한 네트워크에 있어야 하고,
PC 방화벽에서 5000번 포트 접속이 허용돼야 합니다. `10.0.2.2`는 Android 에뮬레이터에서 PC를 가리키는 주소입니다.
Android 17 이상에서 로컬 서버에 접속할 때 앱이 요청하는 주변 기기 권한도 허용합니다.

### PC 서버에서 PostgreSQL로 개발

루트 `.env`에 PostgreSQL `DATABASE_URL`, `DIRECT_URL`을 설정하면 `npm start`도 그 DB를 사용합니다.
개발용 Neon DB/브랜치를 사용하세요. `.env`가 운영 DB를 가리키면 PC에서 저장·삭제한 내용도 운영 데이터에 반영됩니다.
환경변수 변경 후 PC 서버를 재시작합니다. 앱 주소는 여전히 PC 서버 주소이며 DB URL을 앱에 넣지 않습니다.

## 8. 오류가 날 때

| 증상 | 먼저 확인할 것 |
| --- | --- |
| Windows `EPERM`, Prisma 엔진 파일 rename 실패 | 실행 중인 로컬 백엔드를 멈춘 뒤 `npm ci` 또는 `npm run db:generate` 재실행 |
| `DATABASE_URL`/`DIRECT_URL` 누락 | 루트 `.env`와 Vercel 환경변수 각각 설정됐는지 확인 |
| Production JWT 비밀값 오류 | `JWT_SECRET`이 최소 32자이며 개발 기본값이 아닌지 확인 |
| DB 테이블 없음 | 올바른 Neon DB에 `npm run db:migrate:deploy`를 실행했는지 확인 |
| SQLite import 실패 | 원본 DB 경로, 대상 연결 주소, 대상 테이블이 전부 비어 있는지 확인 |
| 브라우저에서 Vercel 로그인 요구 | Production URL의 Deployment Protection 확인 |
| 앱에 결제 연동 설정 오류 | 해당 결제 키·CID, `PAYMENT_PUBLIC_BASE_URL`, 환경변수 변경 후 재배포 여부 확인 |
| 브라우저는 열리지만 페이지가 안 나옴 | 콜백 주소가 기기에서 접근 가능한지, `/api`를 중복으로 넣었는지 확인 |
| 결제창에서 거절됨 | 테스트 키 종류·키 쌍·카카오 도메인 등록, 결제 제공자 오류 문구 확인 |
| 배포 APK가 PC 서버를 찾음 | 클라우드 API 주소를 지정해 APK를 다시 빌드했는지 확인 |
| 로컬 설문은 보이는데 클라우드 목록은 비어 있음 | 실제 원본 DB를 이전했는지, 서버가 같은 Neon DB를 보는지 확인 |

서버 오류는 Vercel 로그의 발생 시각과 앱 오류 시각을 대조합니다.
테스트 성공만으로 실제 결제창 연결이 검증되는 것은 아니므로 배포 후 토스·카카오를 각각 확인합니다.
