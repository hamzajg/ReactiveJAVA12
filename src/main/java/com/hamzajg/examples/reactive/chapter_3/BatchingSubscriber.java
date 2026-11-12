package com.hamzajg.examples.reactive.chapter_3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Chapter 3: a subscriber that decides how much it can take.
 *
 * <p>It asks for one batch in {@code onSubscribe} and then only for more when
 * the consumer calls {@link #requestMore(long)}. Between those calls the
 * publisher must hold items back — that is back-pressure, and it is why a slow
 * subscriber never gets the whole stream pushed into its lap.
 */
public final class BatchingSubscriber<T> implements Flow.Subscriber<T> {

    private final long batchSize;
    private final List<T> received = new CopyOnWriteArrayList<>();
    private final CountDownLatch completed = new CountDownLatch(1);
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final CompletableFuture<Flow.Subscription> subscribed = new CompletableFuture<>();

    public BatchingSubscriber(long batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive: " + batchSize);
        }
        this.batchSize = batchSize;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscribed.complete(subscription);
        subscription.request(batchSize);
    }

    @Override
    public void onNext(T item) {
        received.add(item);
    }

    @Override
    public void onError(Throwable throwable) {
        failure.set(throwable);
        completed.countDown();
    }

    @Override
    public void onComplete() {
        completed.countDown();
    }

    /** Asks upstream for {@code n} more items. */
    public void requestMore(long n) throws Exception {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive: " + n);
        }
        subscribed.get(5, TimeUnit.SECONDS).request(n);
    }

    /** Stops the stream; no further item may be delivered. */
    public void cancel() throws Exception {
        subscribed.get(5, TimeUnit.SECONDS).cancel();
    }

    public List<T> received() {
        return received;
    }

    public Throwable failure() {
        return failure.get();
    }

    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        return completed.await(timeout, unit);
    }
}
