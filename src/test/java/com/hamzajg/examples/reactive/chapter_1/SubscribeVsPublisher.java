package com.hamzajg.examples.reactive.chapter_1;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Chapter 1: a {@link Flow.Publisher} pushes items to a {@link Flow.Subscriber}.
 *
 * <p>The subscriber only receives items after it asks the subscription for
 * them ({@code request}), which is the back-pressure contract of the Flow API.
 */
public class SubscribeVsPublisher {

    @Test
    public void shouldUsePublisherToSendEvent() throws Exception {
        // given
        List<String> received = new CopyOnWriteArrayList<>();
        AtomicInteger delivered = new AtomicInteger();
        CountDownLatch completed = new CountDownLatch(1);

        Flow.Subscriber<String> subscriber = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                // Without request(...) the publisher would never send anything.
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                delivered.incrementAndGet();
                received.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                completed.countDown();
            }

            @Override
            public void onComplete() {
                completed.countDown();
            }
        };

        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()) {
            publisher.subscribe(subscriber);

            // when
            publisher.submit("one");
            publisher.submit("two");
            publisher.submit("three");
            publisher.close();

            // then
            assertTrue("publisher should signal completion",
                    completed.await(5, TimeUnit.SECONDS));
            assertEquals(3, delivered.get());
            assertEquals(List.of("one", "two", "three"), received);
        }
    }
}
