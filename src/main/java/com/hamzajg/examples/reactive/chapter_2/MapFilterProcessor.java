package com.hamzajg.examples.reactive.chapter_2;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Chapter 2: an operator, written as a {@link Flow.Processor}.
 *
 * <p>A processor is a subscriber and a publisher at the same time: it receives
 * items from upstream, transforms them, and pushes the result downstream. This
 * one maps every item, then drops the ones the predicate rejects.
 *
 * <p>It is deliberately single-subscriber. Demand from that subscriber is
 * forwarded upstream, so the pipeline keeps the Flow contract: nothing is
 * produced that was not asked for. A dropped item spent demand without
 * emitting, so one more item is requested to replace it.
 */
public final class MapFilterProcessor<T, R> implements Flow.Processor<T, R> {

    private final Function<T, R> mapper;
    private final Predicate<R> filter;
    private final AtomicLong pendingDemand = new AtomicLong();

    private volatile Flow.Subscription upstream;
    private volatile Flow.Subscriber<? super R> downstream;

    public MapFilterProcessor(Function<T, R> mapper, Predicate<R> filter) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.filter = Objects.requireNonNull(filter, "filter");
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (downstream != null) {
            throw new IllegalStateException("this processor supports a single subscriber");
        }
        downstream = subscriber;
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0) {
                    cancel();
                    return;
                }
                Flow.Subscription current = upstream;
                if (current != null) {
                    current.request(n);
                } else {
                    // Subscribe order is not fixed: remember demand until upstream arrives.
                    pendingDemand.updateAndGet(demand -> demand + n < 0 ? Long.MAX_VALUE : demand + n);
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
        if (upstream != null) {
            subscription.cancel();
            throw new IllegalStateException("already subscribed upstream");
        }
        upstream = subscription;
        long demand = pendingDemand.getAndSet(0);
        if (demand > 0) {
            subscription.request(demand);
        }
    }

    @Override
    public void onNext(T item) {
        R mapped = mapper.apply(item);
        if (!filter.test(mapped)) {
            Flow.Subscription current = upstream;
            if (current != null) {
                current.request(1);
            }
            return;
        }
        Flow.Subscriber<? super R> current = downstream;
        if (current != null) {
            current.onNext(mapped);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Flow.Subscriber<? super R> current = downstream;
        if (current != null) {
            current.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        Flow.Subscriber<? super R> current = downstream;
        if (current != null) {
            current.onComplete();
        }
    }
}
