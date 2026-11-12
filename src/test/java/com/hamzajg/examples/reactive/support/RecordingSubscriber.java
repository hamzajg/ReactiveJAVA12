package com.hamzajg.examples.reactive.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A subscriber that records everything it is given, for the examples to assert on.
 *
 * <p>It requests an unbounded amount in {@code onSubscribe}, so it is the right
 * helper when the example is about transformation rather than demand.
 */
public final class RecordingSubscriber<T> implements Flow.Subscriber<T> {

    private final List<T> items = new CopyOnWriteArrayList<>();
    private final CountDownLatch completed = new CountDownLatch(1);
    private final AtomicReference<Throwable> error = new AtomicReference<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        items.add(item);
    }

    @Override
    public void onError(Throwable throwable) {
        error.set(throwable);
        completed.countDown();
    }

    @Override
    public void onComplete() {
        completed.countDown();
    }

    public List<T> items() {
        return items;
    }

    public Throwable error() {
        return error.get();
    }

    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        return completed.await(timeout, unit);
    }
}
