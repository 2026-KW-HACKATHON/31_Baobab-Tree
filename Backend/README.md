# 백엔드 실행 안내

공통 구현은 저장소 루트의 `src/`, 기준 스키마는 `prisma/schema.prisma`입니다.
이 폴더의 `src/`는 공통 구현을 참조합니다. 백엔드 수정은 루트에서 진행하세요.
스키마를 변경하면 `Backend/prisma/schema.prisma`도 같은 내용으로 맞춰야 하며,
두 스키마의 일치 여부는 통합 테스트에서 확인합니다.

저장소 루트에서 실행합니다.

```powershell
npm ci
npm run db:validate:local
npm run db:generate
npm run db:push:local
npm start
```

기본 주소는 `http://localhost:5000/api`입니다. `.env`에 `PORT`, `JWT_SECRET`을
설정할 수 있습니다. 의존성 설치 후 이 폴더에서 `npm start`, `npm test`를
실행해도 공통 구현을 사용합니다.

`db:push:local`은 실제 로컬 DB를 변경하는 초기 설정 명령입니다. 기존 `nickname`
스키마의 DB가 있다면 `loginId`, `name`을 채우는 별도 데이터 이전이 필요합니다.
이번 작업에서는 기존 DB에 변경 명령을 실행하지 않았습니다.

`npm test`는 별도 임시 SQLite DB에서 API를 검증하고 테스트 후 제거합니다.
요청과 응답 필드는 스키마와 같은 camelCase를 사용합니다.
자세한 계약과 Android 필드 대응은 [API 문서](../docs/api.md)를 참고하세요.

PostgreSQL 운영 배포·데이터 이전은 [배포 안내](../docs/deployment.md)를 참고하세요.
