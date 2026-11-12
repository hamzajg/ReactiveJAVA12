package com.hamzajg.examples.reactive.chapter_3;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Chapter 3: back-pressure. The subscriber asks for a number of items, and the
 * publisher may not deliver more than that, so a fast producer cannot swamp a
 * slow consumer.
 */
public class BackpressureTest {

    private static SubmissionPublisher<String> directPublisher() {
        return new SubmissionPublisher<>(Runnable::run, Flow.defaultBufferSize());
    }

    @Test
    public void onlyRequestedItemsAreDelivered() throws Exception {
        BatchingSubscriber<String> subscriber = new BatchingSubscriber<>(2);

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(subscriber);

            publisher.submit("one");
            publisher.submit("two");
            publisher.submit("three");

            // Two were requested; the third is held back by the publisher.
            assertEquals(List.of("one", "two"), subscriber.received());

            subscriber.requestMore(1);
            assertEquals(List.of("one", "two", "three"), subscriber.received());

            publisher.close();
            assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void cancelledSubscriberReceivesNothingElse() throws Exception {
        BatchingSubscriber<String> subscriber = new BatchingSubscriber<>(1);

        try (SubmissionPublisher<String> publisher = directPublisher()) {
            publisher.subscribe(subscriber);

            publisher.submit("one");
            assertEquals(List.of("one"), subscriber.received());

            subscriber.cancel();
            publisher.submit("two");

            assertEquals(List.of("one"), subscriber.received());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsANonPositiveBatch() {
        new BatchingSubscriber<>(0);
    }
}
