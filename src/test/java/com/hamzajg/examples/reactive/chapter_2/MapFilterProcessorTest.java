package com.hamzajg.examples.reactive.chapter_2;

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
 * Chapter 2: operators. A {@link Flow.Processor} sits between the publisher and
 * the subscriber and transforms what passes through it.
 */
public class MapFilterProcessorTest {

    /**
     * A direct executor makes delivery happen on the submitting thread, so the
     * example is deterministic: no sleeps, no waiting for another thread.
     */
    private static SubmissionPublisher<String> directPublisher() {
        return new SubmissionPublisher<>(Runnable::run, Flow.defaultBufferSize());
    }

    @Test
    public void mapsThenFilters() throws Exception {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();
        MapFilterProcessor<String, Integer> processor =
                new MapFilterProcessor<>(Integer::parseInt, number -> number % 2 == 0);

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(processor);
            processor.subscribe(subscriber);

            publisher.submit("1");
            publisher.submit("2");
            publisher.submit("3");
            publisher.submit("4");
            publisher.close();

            assertTrue("stream should complete", subscriber.awaitCompletion(5, TimeUnit.SECONDS));
        }

        assertEquals(List.of(2, 4), subscriber.items());
        assertNull(subscriber.error());
    }

    @Test
    public void keepsOrderWhenNothingIsFilteredOut() throws Exception {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();
        MapFilterProcessor<String, String> processor =
                new MapFilterProcessor<>(String::toUpperCase, value -> true);

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(processor);
            processor.subscribe(subscriber);

            publisher.submit("alpha");
            publisher.submit("beta");
            publisher.submit("gamma");
            publisher.close();

            assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS));
        }

        assertEquals(List.of("ALPHA", "BETA", "GAMMA"), subscriber.items());
    }

    @Test
    public void forwardsAnUpstreamError() throws Exception {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();
        MapFilterProcessor<String, Integer> processor =
                new MapFilterProcessor<>(Integer::parseInt, number -> true);
        IllegalStateException failure = new IllegalStateException("upstream failed");

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(processor);
            processor.subscribe(subscriber);

            publisher.closeExceptionally(failure);

            assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS));
        }

        assertEquals(failure, subscriber.error());
        assertTrue(subscriber.items().isEmpty());
    }
}
