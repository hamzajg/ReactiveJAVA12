# ReactiveJAVA12

Hands-on examples for *Hands-On Reactive Programming with Java 12*, built on
the JDK's own [Flow API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Flow.html)
(`java.util.concurrent.Flow`).

## Requirements

- JDK 11 or newer (the examples use `var`, `List.of`, and the Flow API)
- No Gradle install needed — the wrapper is checked in (Gradle 8.2)

## Running the tests

```bash
./gradlew test
```

If `./gradlew` is not executable:

```bash
sh gradlew test
```

The HTML report is written to `build/reports/tests/test/index.html`.

## Chapters

| Chapter | Example | What it shows |
|---|---|---|
| 1 | [`SubscribeVsPublisher`](src/test/java/com/hamzajg/examples/reactive/chapter_1/SubscribeVsPublisher.java) | A `SubmissionPublisher` pushing items to a `Flow.Subscriber`, including the `request(n)` back-pressure contract |
| 2 | [`MapFilterProcessor`](src/main/java/com/hamzajg/examples/reactive/chapter_2/MapFilterProcessor.java) | An operator written as a `Flow.Processor`: map then filter between publisher and subscriber, with downstream demand forwarded upstream |
| 3 | [`BatchingSubscriber`](src/main/java/com/hamzajg/examples/reactive/chapter_3/BatchingSubscriber.java) | Back-pressure from the consumer's side: request two items, take two, ask for more, and a `cancel()` that stops delivery |
| 4 | [`ErrorRecoveringProcessor`](src/main/java/com/hamzajg/examples/reactive/chapter_4/ErrorRecoveringProcessor.java) | Errors as part of the stream: `onError` passed through untouched, or turned into a fallback value plus completion |

Each example is a JUnit test: it builds a small pipeline, drives it, and asserts
what the subscriber received. That keeps the examples executable and makes a
regression fail the build.

The tests give `SubmissionPublisher` a *direct* executor (`Runnable::run`), so
items are delivered on the submitting thread. That removes the sleeps and the
`CountDownLatch` races a background executor would need, which is what lets the
back-pressure chapter assert "exactly two items arrived so far".

## Layout

```
src/main/java/com/hamzajg/examples/reactive/
  chapter_2/MapFilterProcessor.java
  chapter_3/BatchingSubscriber.java
  chapter_4/ErrorRecoveringProcessor.java
src/test/java/com/hamzajg/examples/reactive/
  chapter_1/SubscribeVsPublisher.java
  chapter_2/MapFilterProcessorTest.java
  chapter_3/BackpressureTest.java
  chapter_4/ErrorHandlingTest.java
  support/RecordingSubscriber.java
build.gradle
gradlew, gradle/wrapper/
```

## Adding an example

1. Add a package under `src/test/java/com/hamzajg/examples/reactive/chapter_N`.
2. Write a JUnit 4 test that subscribes, drives the publisher, and asserts the
   received items. Always wait with a timeout (`CountDownLatch.await(...)`) so a
   broken pipeline fails instead of hanging.
3. Run `./gradlew test`.

## License

See [LICENSE](LICENSE).
