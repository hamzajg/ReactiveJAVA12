package com.hamzajg.examples.reactive.chapter_4;

import java.util.Objects;
import java.util.concurrent.Flow;

/**
 * Chapter 4: turning a failure into a value.
 *
 * <p>When upstream signals {@code onError}, this processor emits one fallback
 * item and completes. The subscriber never sees the exception, so the stream
 * ends normally — the reactive equivalent of a {@code try/catch} that returns
 * a default. Everything that is not an error passes through untouched.
 *
 * <p>Like the other examples here it is single-subscriber, and the fallback is
 * emitted regardless of outstanding demand: recovery is a terminal event, not
 * a produced item.
 */
public final class ErrorRecoveringProcessor<T> implements Flow.Processor<T, T> {

    private final T fallback;

    private volatile Flow.Subscription upstream;
    private volatile Flow.Subscriber<? super T> downstream;

    public ErrorRecoveringProcessor(T fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (downstream != null) {
            throw new IllegalStateException("this processor supports a single subscriber");
        }
        downstream = subscriber;
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                Flow.Subscription current = upstream;
                if (current != null) {
                    current.request(n);
                }
            }

            @Override
            public void cancel() {
                Flow.Subscription current = upstream;
                if (current != null) {
                    current.cancel();
                }
            }
        });
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        upstream = subscription;
    }

    @Override
    public void onNext(T item) {
        Flow.Subscriber<? super T> current = downstream;
        if (current != null) {
            current.onNext(item);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Flow.Subscriber<? super T> current = downstream;
        if (current == null) {
            return;
        }
        current.onNext(fallback);
        current.onComplete();
    }

    @Override
    public void onComplete() {
        Flow.Subscriber<? super T> current = downstream;
        if (current != null) {
            current.onComplete();
        }
    }
}
