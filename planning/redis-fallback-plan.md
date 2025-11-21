# Redis Health Check & Session Fallback - Design

## Goals
1. RedisService should detect health status, retry, and recover without restarting app.
2. UserSessionService should provide a local fallback (in-memory cache) when Redis is down, with TTL, cleanup, and limits to avoid leaks/security risks.

## Tasks
- RedisService: add AtomicBoolean `redisAvailable`, failure counter, scheduled health check.
- UserSessionService: add `ConcurrentHashMap` for sessions + tokens, store `SessionEntry {UserInfoVO, expireAt}`, scheduled cleanup.
- Logging/metrics: log when switching to fallback or recovering.
- Tests: cover fallback decision logic and health check state transitions.
