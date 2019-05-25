package com.hamzajg.examples.reactive.chapter_1;

import org.junit.Test;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;

public class SubscribeVsPublisher {

    @Test
    public void shouldUsePublisherToSendEvent() {
        // given

        AtomicInteger atomicInteger = new AtomicInteger();
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
        Flow.Subscriber<String> subscriber = new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {

            }

            @Override
            public void onNext(String item) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        };

    }
}
