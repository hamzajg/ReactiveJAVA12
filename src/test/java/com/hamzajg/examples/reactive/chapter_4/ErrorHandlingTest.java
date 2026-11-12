package com.hamzajg.examples.reactive.chapter_4;

import com.hamzajg.examples.reactive.chapter_2.MapFilterProcessor;
import com.hamzajg.examples.reactive.support.RecordingSubscriber;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Chapter 4: failures are part of the stream. A publisher can end a stream with
 * {@code onError} instead of {@code onComplete}, and a processor can decide
 * whether to pass that failure on or turn it into a value.
 */
public class ErrorHandlingTest {

    private static SubmissionPublisher<String> directPublisher() {
        return new SubmissionPublisher<>(Runnable::run, Flow.defaultBufferSize());
    }

    @Test
    public void errorIsRecoveredWithAFallbackValue() throws Exception {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();
        ErrorRecoveringProcessor<String> processor = new ErrorRecoveringProcessor<>("fallback");

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(processor);
            processor.subscribe(subscriber);

            publisher.submit("one");
            publisher.submit("two");
            publisher.closeExceptionally(new IllegalStateException("upstream failed"));

            assertTrue("recovery should complete the stream", subscriber.awaitCompletion(5, TimeUnit.SECONDS));
        }

        assertEquals(List.of("one", "two", "fallback"), subscriber.items());
        assertNull("the subscriber must not see the error", subscriber.error());
    }

    @Test
    public void errorPassesThroughAnOperatorThatDoesNotRecover() throws Exception {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();
        MapFilterProcessor<String, Integer> processor = new MapFilterProcessor<>(Integer::parseInt, number -> true);
        IllegalStateException failure = new IllegalStateException("upstream failed");

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(processor);
            processor.subscribe(subscriber);

            publisher.submit("7");
            publisher.closeExceptionally(failure);

            assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS));
        }

        assertEquals(List.of(7), subscriber.items());
        assertEquals(failure, subscriber.error());
    }

    @Test
    public void recoveryEmitsOnlyTheFallbackWhenNothingWasProduced() throws Exception {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();
        ErrorRecoveringProcessor<String> processor = new ErrorRecoveringProcessor<>("empty");

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(processor);
            processor.subscribe(subscriber);

            publisher.closeExceptionally(new IllegalStateException("failed immediately"));

            assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS));
        }

        assertEquals(List.of("empty"), subscriber.items());
    }
}
