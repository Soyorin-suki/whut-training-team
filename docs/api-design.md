# API Design

This document describes the currently implemented backend API surface.

## 1. Basics

- Direct backend base URL: `http://localhost:8080`
- API prefix: `/api`
- In Docker, browser traffic should normally go through the frontend entrypoint: `http://localhost:5173/api/*`

Unified response envelope:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

Notes:

- Treat `code === 200` as the business success signal
- Many business failures are returned as `HTTP 200` with non-`200` `code`
- Authentication failures usually return `HTTP 401`

Auth headers:

- `Authorization: Bearer <accessToken>`
- `X-Refresh-Token: <refreshToken>`

## 2. Auth Levels

### 2.1 Public endpoints

- `GET /api/health`
- `POST /api/users/register`
- `POST /api/auth/login`

### 2.2 Optional-auth read endpoints

These endpoints can be called anonymously. If a valid access token is present, user-specific status is also resolved.

- `GET /api/problems/{problemKey}`
- `GET /api/problem-comments/{problemKey}`

### 2.3 Login-required endpoints

- user profile endpoints
- daily problem endpoints
- practice endpoints
- likes and favorites
- rankings
- legacy daily comment compatibility endpoints

### 2.4 Admin-only endpoints

- `POST /api/admin/users`
- `POST /api/admin/daily-problem/regenerate`
- `GET /api/admin/training/*`

## 3. Core DTO Snapshots

### 3.1 `ProblemView`

Embedded problem payload used in daily and practice responses:

```json
{
  "type": "DAILY",
  "date": "2026-05-17",
  "problemKey": "2000-A",
  "contestId": 2000,
  "problemIndex": "A",
  "name": "Problem A",
  "rating": 1500,
  "tags": "dp,graphs",
  "sourceUrl": "https://codeforces.com/problemset/problem/2000/A",
  "likeCount": 3,
  "likedByMe": true,
  "favoritedByMe": false,
  "favoritedAt": null
}
```

### 3.2 `ProblemDetailView`

Dedicated problem detail payload:

```json
{
  "problemKey": "2000-A",
  "contestId": 2000,
  "problemIndex": "A",
  "name": "Problem A",
  "rating": 1500,
  "tags": "dp,graphs",
  "sourceUrl": "https://codeforces.com/problemset/problem/2000/A",
  "likeCount": 3,
  "likedByMe": true,
  "favoritedByMe": false,
  "favoritedAt": null
}
```

### 3.3 `ProblemCommentItem`

Problem comments are returned as a two-level tree:

```json
{
  "id": 12,
  "problemKey": "2000-A",
  "content": "Binary search works here.",
  "createdAt": "2026-05-17T09:00:00+08:00",
  "replyCommentId": null,
  "replyToUsername": null,
  "author": {
    "userId": 3,
    "username": "alice",
    "avatarUrl": "https://example.com/avatar.png"
  },
  "replies": []
}
```

## 4. User And Auth Endpoints

### 4.1 Register

- `POST /api/users/register`

Request body:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "123456"
}
```

### 4.2 Login

- `POST /api/auth/login`

Request body:

```json
{
  "username": "alice",
  "password": "123456"
}
```

`data` includes:

- user profile fields
- `score`
- `currentStreakDays`
- `longestStreakDays`
- `accessToken`
- `refreshToken`

### 4.3 Refresh token

- `POST /api/auth/refresh`
- header: `X-Refresh-Token`

### 4.4 Logout

- `POST /api/auth/logout`
- headers: `Authorization` and `X-Refresh-Token`

### 4.5 User profile

- `GET /api/users`
- `GET /api/users/total`
- `GET /api/users/{id}`
- `PATCH /api/users/me`

Update payload:

```json
{
  "username": "alice",
  "email": "new_mail@example.com",
  "password": "new_password_123"
}
```

## 5. Daily Problem And Practice Endpoints

### 5.1 Get today's problem

- `GET /api/daily-problem/today`

Returns:

```json
{
  "problem": {},
  "checkedIn": false,
  "score": null
}
```

Notes:

- `problem` is a `ProblemView`
- the shared daily problem payload may be served from Redis
- `checkedIn` and `score` are still resolved per user

### 5.2 Daily check-in

- `POST /api/daily-problem/check-in`

Request body:

```json
{
  "submissionId": 123456789
}
```

Returns:

```json
{
  "type": "DAILY",
  "accepted": true,
  "submissionId": 123456789,
  "verdict": "OK",
  "score": 1
}
```

### 5.3 Daily history

- `GET /api/daily-problem/history?limit=14`

Each item includes:

- `date`
- `problemKey`
- `name`
- `rating`
- `sourceUrl`
- `checkedIn`
- `submissionId`
- `verdict`
- `score`
- `likeCount`
- `likedByMe`
- `favoritedByMe`
- `favoritedAt`

### 5.4 Practice draw

- `POST /api/practice/draw`

Request body:

```json
{
  "minRating": 1200,
  "maxRating": 1600,
  "tags": "dp,graphs"
}
```

Returns:

```json
{
  "drawId": 1,
  "problem": {}
}
```

`problem` is a `ProblemView`.

### 5.5 Practice check

- `POST /api/practice/check`

Request body:

```json
{
  "drawId": 1,
  "submissionId": 123456789
}
```

### 5.6 Practice history

- `GET /api/practice/history?limit=30`

Each item includes:

- `drawId`
- `drawDate`
- `problemKey`
- `name`
- `rating`
- `sourceUrl`
- `submissionId`
- `verdict`
- `checkedAt`
- `likeCount`
- `likedByMe`
- `favoritedByMe`
- `favoritedAt`

### 5.7 Admin regenerate today's problem

- `POST /api/admin/daily-problem/regenerate`

## 6. Problem Detail, Comments, Likes, Favorites

### 6.1 Problem detail

- `GET /api/problems/{problemKey}`

Notes:

- anonymous reads are allowed
- when logged in, `likedByMe` and `favoritedByMe` are resolved for the current user

### 6.2 Shared problem comments

- `GET /api/problem-comments/{problemKey}`
- `POST /api/problem-comments`

Create payload:

```json
{
  "problemKey": "2000-A",
  "content": "Binary search works here.",
  "replyCommentId": null
}
```

Server-side validation rules:

- `problemKey` is required and must exist
- `content` must be non-blank and at most `1000` chars
- replying to a missing comment returns `404`
- replying across different problems returns `409`

### 6.3 Likes

- `POST /api/problem-like`
- `DELETE /api/problem-like/{problemKey}`

Request body:

```json
{
  "problemKey": "2000-A"
}
```

Response:

```json
{
  "problemKey": "2000-A",
  "likeCount": 3,
  "likedByMe": true
}
```

### 6.4 Favorites

- `POST /api/problem-favorite`
- `DELETE /api/problem-favorite/{problemKey}`
- `GET /api/problem-favorite/mine?page=1&limit=50`

Request body:

```json
{
  "problemKey": "2000-A"
}
```

Status response:

```json
{
  "problemKey": "2000-A",
  "favoritedByMe": true,
  "favoritedAt": "2026-05-17T09:00:00+08:00"
}
```

Paginated favorite list response fields:

- `items`
- `page`
- `limit`
- `total`

Each favorite item includes at least:

- `problemKey`
- `contestId`
- `problemIndex`
- `name`
- `rating`
- `tags`
- `sourceUrl`
- `sourceType`
- `favoritedAt`

## 7. Ranking Endpoints

- `GET /api/rankings?type=DAILY_TOTAL&page=1&pageSize=20`
- `GET /api/rankings/me?type=DAILY_TOTAL`

Supported `type` values:

- `DAILY_TOTAL`
- `SOLVED_COUNT`
- `HARD_SOLVED_COUNT`
- `CURRENT_STREAK`
- `LONGEST_STREAK`

## 8. Admin Endpoints

### 8.1 Admin create user

- `POST /api/admin/users`

Request body:

```json
{
  "username": "student1",
  "email": "student1@example.com",
  "password": "123456",
  "role": "USER"
}
```

### 8.2 Training operations dashboard

- `GET /api/admin/training/overview`
- `GET /api/admin/training/daily-records?startDate=2026-05-01&endDate=2026-05-17&page=1&pageSize=20`
- `GET /api/admin/training/daily-records/{date}`
- `GET /api/admin/training/users?keyword=alice&page=1&pageSize=20`
- `GET /api/admin/training/users/{userId}/timeline?limit=30`

## 9. Legacy Daily Comment Compatibility Endpoints

These endpoints still exist, but the main frontend no longer uses them.

- `GET /api/daily-problem/comments/today`
- `GET /api/daily-problem/comments/archives?limit=30`
- `GET /api/daily-problem/comments?date=2026-05-17&problemKey=2000-A`
- `POST /api/daily-problem/comments`

Compatibility create payload:

```json
{
  "content": "Legacy daily-instance comment",
  "replyCommentId": null,
  "dailyProblemDate": "2026-05-17",
  "problemKey": "2000-A"
}
```

## 10. Common Business Error Codes

- `400` invalid params, blank content, oversized content, mismatched submission
- `401` unauthorized or invalid token
- `403` admin role required
- `404` problem, comment, or user not found
- `409` repeated check-in or cross-problem reply conflict
- `500` internal server error
- `503` upstream Codeforces error
