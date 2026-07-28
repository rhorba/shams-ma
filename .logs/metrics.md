# METRICS — Shams.ma



## 2026-07-22 — Epic 0
- Backend test coverage (JaCoCo instruction ratio): 88% (gate: 80%)
- Frontend test coverage: 100% (trivial placeholder scope, will drop as real features land)
- Backend tests: 17/17 passing
- CI: green (5/5 jobs) as of run 29924938375

## 2026-07-26/27 — Epic 1 SHIP
- Backend coverage: gate passed in CI (JaCoCo >= 80%, exact %% not re-read from CI artifact this session — see Story 1.1-1.3 log entry 2026-07-23 for the last measured figure, 91.55%, which this session's code is additive to).
- Frontend coverage (measured locally, matches CI): 90.27% stmts / 80.68% branch / 85.36% funcs / 91.3% lines — meets the 80% gate.

## 2026-07-28 — Epic 2 SHIP (ROI calculator + address-based browse)
- Backend: full suite (59 tests incl. all Testcontainers integration classes, real Docker via PowerShell) confirmed GREEN exit-code-0 earlier this session (see activity.md Batch 2/3 entries); `mvn compile` + `spotless:check` re-confirmed clean at SHIP time.
- Frontend: 9 test files / 26 tests passing. Coverage 94.31% stmts / 85.71% branch / 92.59% funcs / 95.26% lines (lcov-verified — istanbul's printed text-report table cosmetically drops the `src/features/roi`, `src/test-utils.tsx`, and `src/theme.ts` rows, but their line counts ARE included in the totals, confirmed against raw lcov.info sums matching the reported 161/169 lines). Meets the 80% gate.
- Secret spot-check: diffed all changed + new files for hardcoded secrets/keys/passwords — none found.
