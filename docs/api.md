# Survey API contract

The source of truth is `prisma/schema.prisma` at the repository root.
The API uses the Prisma field names in camelCase. SQL names declared with
`@map` are database column names, not JSON keys.

## Run

Run these commands from the repository root:

```powershell
npm ci
npm run db:validate
npm run db:generate
npm run db:push
npm start
```

`db:push` creates/synchronizes the local SQLite database (`prisma/dev.db`).
It is an explicit setup command; tests do not modify this database.
For an existing database, migrate its data before adding required `loginId`
and `name` fields. No automatic conversion from the old `nickname` schema
is performed.

The default URL is `http://localhost:5000/api`. Set `PORT` and `JWT_SECRET`
in a local `.env` file or the environment. Without `JWT_SECRET`, the server
uses a local development secret. Use a private secret for deployment.

`Backend/src` contains compatibility entry points referencing the root code.
`Backend/prisma/schema.prisma` mirrors the root schema, checked by tests.
Install dependencies at the root. `npm start` and `npm test` also work from
`Backend`; its `db:*` scripts delegate to the root.

## JSON and authentication

- Send `Content-Type: application/json`.
- Protected routes require `Authorization: Bearer <accessToken>`.
- The server obtains `userId` from JWT; body `userId` is not used as identity.
- Errors have the shape `{ "error": "message" }`.
- Dates use ISO 8601 strings in responses.
- Passwords are hashed and never included in responses.

## Routes

| Method | Path (under /api) | Authentication | Success |
| --- | --- | --- | --- |
| POST | /auth/signup | No | 201 |
| POST | /auth/login | No | 200 |
| POST | /auth/logout | Yes | 200 |
| GET | /users/me | Yes | 200 |
| PATCH | /users/me | Yes | 200 |
| GET | /users/me/responses | Yes | 200 |
| GET | /users/me/points | Yes | 200 |
| GET | /surveys | No | 200 |
| GET | /surveys/:id | No | 200 |
| POST | /surveys | Yes | 201 |
| PATCH | /surveys/:id | Author | 200 |
| DELETE | /surveys/:id | Author | 200 |
| POST | /surveys/:id/questions | Author | 201 |
| PATCH | /questions/:id | Author | 200 |
| DELETE | /questions/:id | Author | 200 |
| POST | /surveys/:id/responses | Yes | 201 |
| GET | /surveys/:id/results | Author | 200 |

## Signup and login

Signup requires all four schema-required user fields plus a password:

```json
{
  "email": "author@example.com",
  "loginId": "author",
  "name": "Author",
  "password": "password123",
  "ageGroup": "20s",
  "region": "Seoul"
}
```

`ageGroup` and `region` are optional. Signup returns `{ "message": "Signed up",
"user": { "id": 1, "email": "author@example.com", "loginId": "author",
"name": "Author", "ageGroup": "20s", "region": "Seoul", "point": 0,
"createdAt": "..." } }`.

Login accepts `{ "loginId": "author", "password": "password123" }`.
`email` may be supplied instead of `loginId`.
It returns `{ "accessToken": "...", "user": { ... } }`; JWT expires in one day.
`/users/me` returns the same public user fields.
Logout instructs the client to discard the JWT. It does not revoke existing
JWTs on the server; they remain valid until expiration.

## Create / update / read surveys

```json
{
  "title": "Travel survey",
  "category": "Life",
  "rewardPoint": 300,
  "targetCount": 100,
  "endDate": "2026-12-31T23:59:59+09:00",
  "questions": [
    {
      "question": "Bus or walk?",
      "questionType": "single",
      "options": [{ "optionText": "Bus" }, { "optionText": "Walk" }]
    },
    { "question": "Why?", "questionType": "short", "options": [] }
  ]
}
```

- `title` and a nonempty `questions` array are required for creation.
- `questionType` is `single` (default) or `short`.
- A `single` question needs at least two distinct, nonempty options.
- A `short` question has no options; omitting its options is allowed.
- `rewardPoint` and `targetCount` are nonnegative integers (default 0).
- `targetCount: 0` means no participation limit.
- `status` is `OPEN` (default) or `CLOSED`.
- `endDate` is optional; `null` removes a deadline.
- PATCH accepts `title`, `category`, `rewardPoint`, `targetCount`, `status`,
  and `endDate`. It does not replace questions.
- Question routes use `question`, `questionType`, `options[].optionText`.
  Questions cannot change after the survey receives responses. The final
  question cannot be deleted.

Creation, update, list and detail return the same survey shape:

```json
{
  "id": 1,
  "userId": 1,
  "title": "Travel survey",
  "category": "Life",
  "rewardPoint": 300,
  "targetCount": 100,
  "currentCount": 0,
  "status": "OPEN",
  "endDate": "2026-12-31T14:59:59.000Z",
  "createdAt": "...",
  "author": { "id": 1, "name": "Author" },
  "questions": [
    {
      "id": 10,
      "surveyId": 1,
      "question": "Bus or walk?",
      "questionType": "single",
      "options": [{ "id": 20, "questionId": 10, "optionText": "Bus" }]
    }
  ]
}
```

The example response abbreviates options/questions; the actual response
contains every question and option, ordered by ID.
GET `/surveys` returns an array in descending ID order and supports `category`,
`status`, and `search` query parameters. Search matches title or author name;
SQLite determines case sensitivity.

## Submit answers and receive a reward

```json
{
  "answers": [
    { "questionId": 10, "answer": "Bus" },
    { "questionId": 11, "answer": "Convenient" }
  ]
}
```

Every required question must be answered exactly once with a nonempty string. Optional questions may be omitted from answers; do not submit empty answer strings.
A single-choice answer is the option text, not its ID. IDs must belong to
this survey; duplicate and foreign question IDs are rejected.

One transaction checks survey status/deadline, increments `currentCount`
within the participation limit, saves response/answers, increments `User.point`,
and records `PointHistory.amount` and `PointHistory.description`.
A unique `(userId, surveyId)` constraint prevents repeat rewards.

```json
{
  "message": "Response submitted",
  "responseId": 1,
  "rewardPoint": 300,
  "point": 300
}
```

GET `/users/me/points` returns `{ "point": 300, "histories": [...] }`.
Each history has `id`, `userId`, `amount`, `description`, `createdAt`.
GET `/users/me/responses` returns response records with their survey and answers.

GET `/surveys/:id/results` returns:

```json
{
  "surveyId": 1,
  "totalResponses": 1,
  "questions": [
    {
      "questionId": 10,
      "results": [{ "option": "Bus", "count": 1, "percentage": 100 }]
    }
  ]
}
```

## Android mapping and schema limits

Map `id` to the app's survey ID, `author.name` to author, `rewardPoint` to
point display, `currentCount` to participant count, `endDate` to deadline,
and `questions.length` to question count. The actual question and option IDs
from detail must be retained for submission.

The schema stores optional `description`, `audience`, `duration`, and `imageData`.
`imageData` is a JPEG, PNG or WebP base64 data URI (up to 1,000,000 characters); null clears the image.
Text metadata accepts up to 5,000 characters per field; null clears it.
Creation and survey PATCH accept these fields and read responses include them.
Question `required` is a boolean, defaults to true, and is accepted by creation and question routes.
Results include per-question `responseCount`; percentages use that question's answered count, excluding skipped optional answers.
For existing current-schema databases run `npm run db:migrate:survey-fields` before
`npm run db:generate`. The migration creates a dated SQLite backup and preserves existing rows.

## Errors and tests

- 400: missing/invalid input, closed/expired/full survey, invalid answer.
- 401: invalid credentials or missing/invalid/expired JWT.
- 403: author-only action attempted by another user.
- 404: missing record or endpoint.
- 409: duplicate email/loginId, repeat participation, editing answered questions.
- 500: unexpected server error (database details are not exposed).

`npm test` creates an isolated temporary SQLite database and starts the API
on an ephemeral local port. It verifies authentication, Prisma writes,
ownership, answer validation, rewards, duplicates, deadlines, capacity,
transaction rollback, and the compatibility entry points. The temporary
database is removed after testing.

## Account profile editing and backend compatibility

`PATCH /api/users/me` accepts `name`, `email`, `ageGroup`, and `region`.
Name and email must be nonempty; email must have a valid format and remain unique.
Region and ageGroup may be null to clear them. Unknown fields, including user ID,
login ID, password and point, are rejected. JWT identifies the account to update.
The response is the public user object used by `GET /users/me`.

The merged backend branch additionally supports legacy request aliases:
`reward_point`, `target_headcount` / `target_count`, `end_date`,
`question_type`, question `text`, string options or `option_text` / `text`,
and answer `question_id` / `value`. Responses retain the Android camelCase contract.
Email and author-only result access are preserved for compatibility with existing
accounts and the Android app. A production server must set `JWT_SECRET`.

Android sharing uses `baobab://surveys/<id>` and the Android Sharesheet.
A recipient needs the BAOBAB app and network access to the configured API server.
No public web landing page is deployed yet.

Production configuration and Android session behavior: see [deployment setup](deployment.md).
