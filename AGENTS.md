# AGENTS.md — ReactiveJAVA12

Guidance for AI agents (and humans) working in this repository.

## Project

Hands-on examples for *Hands-On Reactive Programming with Java 12*, built on
the JDK's own Flow API (`java.util.concurrent.Flow`) — reactive streams
without Reactor/RxJava. Learning repo: each example is a runnable JUnit
test.
Stack: Java (Gradle wrapper), JUnit 4, `SubmissionPublisher`,
`Flow.Processor`/`Flow.Subscriber`.

## Commands

Verified 2026-11-13 against CI (`.github/workflows/ci.yml`) and README.

| Task | Command |
|---|---|
| Build + test | `sh gradlew build --no-daemon` (CI: JDK 17 launcher) |
| Tests only | `sh gradlew test` (or `./gradlew test` if executable) |
| Test report | `build/reports/tests/test/index.html` |

Requires: JDK to run Gradle (CI uses 17). **Compilation toolchain is JDK 25**
(`build.gradle` → `java.toolchain.languageVersion = 25`), auto-resolved.

## Architecture

```
src/main/java/com/hamzajg/examples/reactive/
  chapter_2/MapFilterProcessor.java       Flow.Processor: map→filter,
                                          forwards downstream demand upstream
  chapter_3/BatchingSubscriber.java       consumer-side back-pressure:
                                          request(2), take 2, request more,
                                          cancel()
  chapter_4/ErrorRecoveringProcessor.java errors in-stream: pass-through
                                          onError or fallback + complete
src/test/java/com/hamzajg/examples/reactive/
  chapter_1/SubscribeVsPublisher.java     SubmissionPublisher → Subscriber,
                                          request(n) contract
  chapter_2/MapFilterProcessorTest.java
  chapter_3/BackpressureTest.java
  chapter_4/ErrorHandlingTest.java
  support/RecordingSubscriber.java        shared test subscriber
build.gradle / gradlew(bat)
```

## Conventions

- **Every example is a JUnit test** — build a pipeline, drive it, assert
  what the subscriber received. New examples follow the README's "Adding
  an example" recipe (package per chapter, `CountDownLatch.await(timeout)`
  so broken pipelines fail, not hang).
- **Direct executor everywhere**: tests give `SubmissionPublisher` a
  `Runnable::run` executor so delivery is synchronous on the submitting
  thread — this is what makes back-pressure assertions deterministic. Don't
  swap in a background executor without rewriting the timing assumptions.
- JDK Flow API only — no Reactor/RxJava. JUnit 4 (`junit:junit:4.12`).
- Chapter = package under `examples/reactive/chapter_N`.

## Landmines

- **Default branch is `master`, not `main`.**
- JDK 17 launcher / JDK 25 toolchain split — don't "fix" the numbers.
- `RecordingSubscriber` in `support/` is shared test infra — reuse it.
- Gradle 6.9.x-era wrapper with toolchain 25: `test { exclude '**/support/**' }`
  may be needed if Gradle can't read v69 classes for filtering (seen in the
  JDK-25 sweep) — check `build.gradle` test block before touching it.
- Chapter 1's example lives in `src/test` (it's a test-only demo), others
  in `src/main` — match the existing placement.

## Agent rules

- Branch from `master`; name branches `feat/*`, `fix/*`, `chore/*`, `docs/*`.
- Docs-only changes still go through a PR; never commit to `master` directly.
- Verify before claiming: `sh gradlew test` and report the real result.
- Keep changes minimal and consistent with surrounding code. No drive-by
  refactors.
- New reactive examples must be tests-first per the README recipe.
- Update AGENTS.md / docs/ARCHITECTURE.md when adding a chapter or changing
  the executor/test strategy.
