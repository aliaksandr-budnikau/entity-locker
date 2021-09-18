package org.locker.tests;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locker.BasicEntityLocker;
import org.locker.EscalationEntityLocker;
import org.locker.GlobalEntityLocker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.random;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalEntityLockerTest {

    private GlobalEntityLocker<Integer> locker;
    private volatile int counter;
    private CountDownLatch countDownLatchStopper;
    private ReentrantLock stopperLock;

    @BeforeEach
    void setUp() {
        counter = 0;
        locker = new GlobalEntityLocker<>(new EscalationEntityLocker<>(new BasicEntityLocker<>(), 3));
        countDownLatchStopper = new CountDownLatch(1);
        stopperLock = new ReentrantLock();
    }

    @Test
    @SneakyThrows
    void globallyLockedAndUnlocked_noTimeout() {
        stopperLock.lock();
        int timeout = 500;

        runAsync(() -> {
            lock();
            unlock();
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertTrue(supplyAsync(() -> {
            try {
                return tryLock(timeout);
            } finally {
                unlock();
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
    void globallyLockedWithTimeoutAndUnlocked_noTimeout() {
        stopperLock.lock();
        int timeout = 500;

        runAsync(() -> {
            tryLock(timeout);
            unlock();
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertTrue(supplyAsync(() -> {
            try {
                return tryLock(timeout);
            } finally {
                unlock();
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
    void globallyLocked_timeout() {
        stopperLock.lock();
        int timeout = 5;

        runAsync(() -> {
            lock();
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertFalse(supplyAsync(() -> tryLock(timeout)).join());
        assertFalse(supplyAsync(() -> tryLock(2, timeout)).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void globallyLockedWithTimeout_timeout() {
        stopperLock.lock();
        int timeout = 5;

        runAsync(() -> {
            tryLock(timeout);
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertFalse(supplyAsync(() -> tryLock(timeout)).join());
        assertFalse(supplyAsync(() -> tryLock(2, timeout)).join());

        stopperLock.unlock();
    }

    @Test
    @SneakyThrows
    void entityLockedAndUnlocked_noTimeout() {
        stopperLock.lock();
        int timeout = 5;

        runAsync(() -> {
            int id = 1;
            lock(id);
            unlock(id);
            countDownLatchStopper.countDown();
            stopperLock.lock();
        });

        countDownLatchStopper.await();
        assertTrue(supplyAsync(() -> {
            try {
                return tryLock(timeout);
            } finally {
                unlock();
            }
        }).join());
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
            try {
                return tryLock(timeout);
            } finally {
                unlock();
            }
        }).join());
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
        assertFalse(supplyAsync(() -> tryLock(timeout)).join());
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
        assertFalse(supplyAsync(() -> tryLock(timeout)).join());
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

    @Test
    void reentrantLocking_lock() {
        lock();
        try {
            lock();
            try {
                lock();
                try {
                    counter++;
                } finally {
                    unlock();
                }
            } finally {
                unlock();
            }
        } finally {
            unlock();
        }
        assertEquals(1, counter);
    }

    @Test
    void reentrantLocking_tryLock() {
        tryLock(100);
        try {
            tryLock(100);
            try {
                tryLock(100);
                try {
                    counter++;
                } finally {
                    unlock();
                }
            } finally {
                unlock();
            }
        } finally {
            unlock();
        }
        assertEquals(1, counter);
    }

    @SneakyThrows
    private void doIncrements(int endExclusive) {
        int i = endExclusive;
        while (i-- != 0) {
            int numberOfLocks = (int) (random() * 3 + 1);
            for (int j = 0; j < numberOfLocks; j++) {
                int lockType = (int) (random() * 2);
                if (lockType == 0) {
                    locker.lock();
                } else {
                    locker.tryLock(1, HOURS);
                }
            }
            try {
                counter++;
            } finally {
                for (int j = 0; j < numberOfLocks; j++) {
                    unlock();
                }
            }
        }
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
    private void lock() {
        locker.lock();
    }

    @SneakyThrows
    private void unlock() {
        locker.unlock();
    }

    @SneakyThrows
    private boolean tryLock(int timeout) {
        return locker.tryLock(timeout, MILLISECONDS);
    }

    @SneakyThrows
    private boolean tryLock(int timeout, TimeUnit unit) {
        return locker.tryLock(timeout, unit);
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

