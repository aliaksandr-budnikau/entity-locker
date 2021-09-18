package org.locker;

import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;

public class ConcurrentUtils {

    @SneakyThrows
    public static void await(CountDownLatch countDownLatch) {
        countDownLatch.await();
    }

    @SneakyThrows
    public static void countDown(CountDownLatch countDownLatch) {
        countDownLatch.countDown();
    }
}
