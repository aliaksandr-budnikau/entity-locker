package org.locker.tests;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locker.BasicEntityLocker;
import org.locker.EscalationEntityLocker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.random;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscalationEntityLockerTest {

    public static final int THRESHOLD = 3;
    private EscalationEntityLocker<Integer> locker;
    private volatile int counter;
    private ReentrantLock stopperLock;
    private CountDownLatch countDownLatchStopper;

    @BeforeEach
    void setUp() {
        counter = 0;
        locker = new EscalationEntityLocker<>(new BasicEntityLocker<>(), THRESHOLD);
        countDownLatchStopper = new CountDownLatch(1);
        stopperLock = new ReentrantLock();
    }

    @Test
    @SneakyThrows
    void globallyLockedOverThresholdAndUnlocked_noTimeout() {
        stopperLock.lock();
        int timeout = 500;

        runAsync(() -> {
            lockOverThreshold();
            unlockAll();
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void globallyLockedOverThresholdWithTimeoutAndUnlocked_noTimeout() {
        stopperLock.lock();
        int timeout = 500;

        runAsync(() -> {
            tryLockOverThreshold(timeout);
            unlockAll();
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void globallyLockedOverThreshold_timeout() {
        stopperLock.lock();
        int timeout = 5;

        runAsync(() -> {
            lockOverThreshold();
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertFalse(supplyAsync(() -> tryLock(2, timeout)).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void globallyLockedOverThresholdWithTimeout_timeout() {
        stopperLock.lock();

        int timeout = 5;
        runAsync(() -> {
            tryLockOverThreshold(timeout);
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertFalse(supplyAsync(() -> tryLock(2, timeout)).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void entityLockedAndUnlocked_noTimeout() {
        stopperLock.lock();
        int timeout = 500;

        runAsync(() -> {
            int id = 1;
            lock(id);
            unlock(id);
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertTrue(supplyAsync(() -> {
            int id = 1;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void entityLockedWithTimeoutAndUnlocked_noTimeout() {
        stopperLock.lock();
        int timeout = 500;

        runAsync(() -> {
            int id = 1;
            tryLock(id, timeout);
            unlock(id);
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertTrue(supplyAsync(() -> {
            int id = 1;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void entityLocked_timeout() {
        stopperLock.lock();
        int timeout = 5;

        runAsync(() -> {
            int id = 1;
            lock(id);
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertFalse(supplyAsync(() -> tryLock(1, timeout)).join());
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void entityLockedWithTimeout_timeout() {
        stopperLock.lock();
        int timeout = 50;

        runAsync(() -> {
            int id = 1;
            tryLock(id, timeout);
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertFalse(supplyAsync(() -> tryLock(1, timeout)).join());
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void sync() {
        int endExclusive = 10000;

        Thread thread1 = new Thread(() -> doIncrements(endExclusive));
        thread1.start();

        Thread thread2 = new Thread(() -> doIncrements(endExclusive));
        thread2.start();

        Thread thread3 = new Thread(() -> doIncrements(endExclusive));
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();
        assertEquals(endExclusive * 3, counter);
    }

    @SneakyThrows
    private void doIncrements(int endExclusive) {
        int i = endExclusive;
        while (i-- != 0) {
            int numberOfLocks = (int) (random() * 3 + 1);
            for (int j = 0; j < numberOfLocks; j++) {
                int lockType = (int) (random() * 2);
                if (lockType == 0) {
                    locker.lock(1);
                } else {
                    locker.tryLock(1, 1, HOURS);
                }
            }
            try {
                counter++;
            } finally {
                for (int j = 0; j < numberOfLocks; j++) {
                    unlock(1);
                }
            }
        }
    }

    @SneakyThrows
    private void lockOverThreshold() {
        for (int i = 0; i <= THRESHOLD; i++) {
            locker.lock(i);
        }
    }

    @SneakyThrows
    private void lock(int id) {
        locker.lock(id);
    }

    private void unlock(int id) {
        locker.unlock(id);
    }

    private void unlockAll() {
        for (int i = THRESHOLD; i >= 0; i--) {
            locker.unlock(i);
        }
    }

    @SneakyThrows
    private boolean tryLockOverThreshold(int timeout) {
        boolean result = false;
        for (int i = 0; i <= THRESHOLD; i++) {
            result = locker.tryLock(i, timeout, MILLISECONDS);
            if (!result) {
                return false;
            }
        }
        return result;
    }

    @SneakyThrows
    private boolean tryLock(int id, long timeout) {
        return locker.tryLock(id, timeout, MILLISECONDS);
    }
}
