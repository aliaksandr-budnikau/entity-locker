package org.locker;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.random;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicEntityLockerTest {
    private BasicEntityLocker<Integer> locker;
    private volatile int counter;
    private CountDownLatch countDownLatchStopper;
    private Lock stopperLock;

    @BeforeEach
    void setUp() {
        counter = 0;
        locker = new BasicEntityLocker<>();
        countDownLatchStopper = new CountDownLatch(1);
        stopperLock = new ReentrantLock();
    }

    @Test
    @SneakyThrows
    void sync() {
        int endExclusive = 100000;

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
        assertEquals(0, locker.getLocksNumber());
    }

    @Test
    void reentrantLocking() {
        lock(1);
        try {
            lock(1);
            try {
                counter++;
            } finally {
                unlock(1);
            }
        } finally {
            unlock(1);
        }

        assertEquals(1, counter);
        assertEquals(0, locker.getLocksNumber());
    }

    @Test
    void reentrantLockingOverOtherId() {
        lock(1);
        try {
            lock(2);
            try {
                lock(1);
                try {
                    counter++;
                } finally {
                    unlock(1);
                }
            } finally {
                unlock(2);
            }
        } finally {
            unlock(1);
        }
        assertEquals(1, counter);
        assertEquals(0, locker.getLocksNumber());
    }

    @Test
    @SneakyThrows
    void timeout() {
        stopperLock.lock();

        runAsync(() -> {
            lock(1);
            try {
                countDownLatchStopper.countDown();
                stopperLock.lock();
            } finally {
                unlock(1);
            }
        });

        countDownLatchStopper.await();
        CompletableFuture<Boolean> future = supplyAsync(() -> {
            try {
                return tryLock(1, 10, MILLISECONDS);
            } catch (Exception e) {
            }
            return true;
        });

        assertFalse(future.join());

        stopperLock.unlock();
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

    @SneakyThrows
    private void lock(int id) {
        locker.lock(id);
    }

    @SneakyThrows
    private void unlock(int id) {
        locker.unlock(id);
    }

    @SneakyThrows
    private boolean tryLock(int id, int timeout) {
        return locker.tryLock(id, timeout, MILLISECONDS);
    }

    @SneakyThrows
    private boolean tryLock(int id, int timeout, TimeUnit unit) {
        return locker.tryLock(id, timeout, unit);
    }

}
