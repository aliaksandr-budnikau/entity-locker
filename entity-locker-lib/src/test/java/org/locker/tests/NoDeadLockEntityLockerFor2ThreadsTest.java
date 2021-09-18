package org.locker.tests;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locker.DeadlockDetectedException;
import org.locker.EscalationSyncEntityLocker;
import org.locker.GlobalSyncEntityLocker;
import org.locker.NoDeadLockEntityLocker;
import org.locker.SyncEntityLocker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.locker.ConcurrentUtils.await;
import static org.locker.ConcurrentUtils.countDown;

/**
 * t1 == thread1
 * r1 == resource1
 */
class NoDeadLockEntityLockerFor2ThreadsTest {
    public static final int N_THREADS = 3;
    private NoDeadLockEntityLocker<Integer> locker;
    private CountDownLatch countDownLatch;
    private ExecutorService threadPool;

    @Test
    public void reproduceDeadlock_t1_has_r1_yet_pending_r2_and_t2_has_r2_yet_pending_r1() {
        threadPool = newFixedThreadPool(N_THREADS);
        for (int i = 0; i < 100000; i++) {
            reproduceDeadlock();
        }
        threadPool.shutdown();
    }

    private void reproduceDeadlock() {
        locker = new NoDeadLockEntityLocker<>(new EscalationSyncEntityLocker<>(new GlobalSyncEntityLocker<>(new SyncEntityLocker<>(null)), 100));
        countDownLatch = new CountDownLatch(2);
        assertThrows(DeadlockDetectedException.class, () -> {
            CompletableFuture<Void> future1 = runAsync(() -> {
                lock(1);
                try {
                    countDown(countDownLatch);
                    await(countDownLatch);
                    lock(2);
                    unlock(2);
                } finally {
                    unlock(1);
                }
            }, threadPool);

            CompletableFuture<Void> future2 = runAsync(() -> {
                lock(2);
                try {
                    countDown(countDownLatch);
                    await(countDownLatch);
                    lock(1);
                    unlock(1);
                } finally {
                    unlock(2);
                }
            }, threadPool);

            try {
                future1.join();
            } catch (CompletionException e) {
                future2.join();
                throw e.getCause();
            }

            try {
                future2.join();
            } catch (CompletionException e) {
                throw e.getCause();
            }
        });
    }

    private void unlock(int id) {
        locker.unlock(id);
    }

    @SneakyThrows
    private void lock(int id) {
        locker.lock(id);
    }

}
