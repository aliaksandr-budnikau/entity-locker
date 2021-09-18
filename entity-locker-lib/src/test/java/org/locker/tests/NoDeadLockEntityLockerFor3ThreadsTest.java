package org.locker.tests;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locker.BasicEntityLocker;
import org.locker.DeadlockDetectedException;
import org.locker.EscalationEntityLocker;
import org.locker.NoDeadLockEntityLocker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * t1 == thread1
 * r1 == resource1
 */
class NoDeadLockEntityLockerFor3ThreadsTest {
    public static final int N_THREADS = 3;
    private NoDeadLockEntityLocker<Integer> locker;
    private CountDownLatch countDownLatchStopper;
    private ExecutorService threadPool;

    @Test
    public void reproduceDeadlock_t1_has_r1_yet_pending_r2_and_t2_has_r2_yet_pending_r3_and_t3_has_r3_yet_pending_r1() {
        threadPool = newFixedThreadPool(N_THREADS);
        for (int i = 0; i < 10000; i++) {
            reproduceDeadlock();
        }
        threadPool.shutdown();
    }

    private void reproduceDeadlock() {
        locker = new NoDeadLockEntityLocker<>(new EscalationEntityLocker<>(new BasicEntityLocker<>(), 100));
        countDownLatchStopper = new CountDownLatch(N_THREADS);
        assertThrows(DeadlockDetectedException.class, () -> {
            CompletableFuture<Void> future1 = runAsync(() -> {
                lock(1);
                try {
                    countDown();
                    await();
                    lock(2);
                    unlock(2);
                } finally {
                    unlock(1);
                }
            }, threadPool);

            CompletableFuture<Void> future2 = runAsync(() -> {
                lock(2);
                try {
                    countDown();
                    await();
                    lock(3);
                    unlock(3);
                } finally {
                    unlock(2);
                }
            }, threadPool);

            CompletableFuture<Void> future3 = runAsync(() -> {
                lock(3);
                try {
                    countDown();
                    await();
                    lock(1);
                    unlock(1);
                } finally {
                    unlock(3);
                }
            }, threadPool);

            try {
                future1.join();
            } catch (CompletionException e) {
                future2.join();
                future3.join();
                throw e.getCause();
            }

            try {
                future2.join();
            } catch (CompletionException e) {
                future3.join();
                throw e.getCause();
            }

            try {
                future3.join();
            } catch (CompletionException e) {
                throw e.getCause();
            }
        });
    }

    @SneakyThrows
    private void await() {
        countDownLatchStopper.await();
    }

    @SneakyThrows
    private void countDown() {
        countDownLatchStopper.countDown();
    }

    private void unlock(int id) {
        locker.unlock(id);
    }

    @SneakyThrows
    private void lock(int id) {
        locker.lock(id);
    }

}
