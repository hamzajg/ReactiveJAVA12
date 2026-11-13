# ADR 0001 — Initial architecture

Date: 2026-11-13 (baseline recorded retroactively)
Status: accepted (describes what is built)

## Context

A companion repo to *Hands-On Reactive Programming with Java 12*, using
the JDK's Flow API instead of a reactive library.

## Decisions

1. **JDK Flow API, no Reactor/RxJava.** `java.util.concurrent.Flow`
   publishers/processors/subscribers. Rationale: the book's subject is the
   standard API — a library would hide the mechanics.
2. **Every example is a JUnit test.** Chapters are test classes that build,
   drive, and assert pipelines. Rationale: executable, self-verifying
   examples; regressions fail CI.
3. **Direct executor in tests.** `SubmissionPublisher` runs items on the
   submitting thread via `Runnable::run`. Rationale: deterministic
   back-pressure assertions without timing flakes.
4. **Chapter-per-package layout.** `chapter_N` packages under
   `examples/reactive`; shared test support in `support/`. Rationale:
   mirrors the book's structure.
5. **JUnit 4 only.** `junit:junit:4.12` — period-appropriate for the
   Java-12-era book. Rationale: consistency with the source material.
6. **Gradle wrapper + JDK 25 toolchain.** Checked-in `gradlew`; compile
   toolchain 25, CI launcher JDK 17. Rationale: fleet SDK standardisation
   without burdening contributors.

## Consequences

- Examples can't be "run" standalone — they're tests; the report is the
  output.
- Synchronous delivery is a test convenience, not a production pattern —
  documented so nobody copies it into real async code blindly.
- Chapter 1 lives under `src/test` while 2–4 are under `src/main` —
  historical placement; harmless but inconsistent.
