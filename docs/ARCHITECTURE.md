# Architecture — ReactiveJAVA12

Current-state map. Descriptive, not aspirational; see `docs/adr/` for why.

## Shape

```
ReactiveJAVA12/   (Gradle, single module, JDK 25 toolchain)
├── build.gradle    junit 4.12 test dep
├── gradlew(bat)
└── src/
    ├── main/java/com/hamzajg/examples/reactive/
    │   ├── chapter_2/MapFilterProcessor.java
    │   ├── chapter_3/BatchingSubscriber.java
    │   └── chapter_4/ErrorRecoveringProcessor.java
    └── test/java/com/hamzajg/examples/reactive/
        ├── chapter_1/SubscribeVsPublisher.java   (test-only example)
        ├── chapter_2..4/*Test.java               executable examples
        └── support/RecordingSubscriber.java
```

## The Flow API coverage

| Chapter | Construct | Demonstrates |
|---|---|---|
| 1 | `SubmissionPublisher` + `Flow.Subscriber` | push model, `request(n)` back-pressure |
| 2 | `Flow.Processor` (`MapFilterProcessor`) | operator between publisher/subscriber, demand forwarding |
| 3 | `Flow.Subscriber` (`BatchingSubscriber`) | consumer-driven demand, batching, `cancel()` |
| 4 | `Flow.Processor` (`ErrorRecoveringProcessor`) | `onError` pass-through vs fallback+complete |

## Design

- **Examples ARE tests.** Each chapter is a JUnit test driving a real
  pipeline — executable documentation that fails the build on regression.
- **Synchronous execution.** `SubmissionPublisher` gets a direct executor
  (`Runnable::run`) in tests — deterministic, no latches/races for
  delivery timing (latches only await final state with timeouts).
- **Support class shared.** `RecordingSubscriber` captures items/errors
  for assertions across chapters.
- **No reactive library.** `java.util.concurrent.Flow` only — the JDK is
  the subject.

## Verification

`sh gradlew test` → `build/reports/tests/test/index.html`. CI runs
`sh gradlew build --no-daemon` on a JDK 17 launcher with the JDK 25
toolchain auto-resolved.
