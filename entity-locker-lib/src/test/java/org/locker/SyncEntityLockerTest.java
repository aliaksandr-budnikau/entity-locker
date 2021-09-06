package org.locker;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.random;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncEntityLockerTest {
    private SyncEntityLocker<Integer> locker;
    private volatile int counter;

    @BeforeEach
    void setUp() {
        counter = 0;
        locker = new SyncEntityLocker<>(null);
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
        //assertEquals(0, locker.getLocksNumber());
    }

    @Test
    void reentrantLocking() {
        locker.lock(1);
        try {
            locker.lock(1);
            try {
                counter++;
            } finally {
                locker.unlock(1);
            }
        } finally {
            locker.unlock(1);
        }

        assertEquals(1, counter);
        //assertEquals(0, locker.getLocksNumber());
    }

    @Test
    void reentrantLockingOverOtherId() {
        locker.lock(1);
        try {
            locker.lock(2);
            try {
                locker.lock(1);
                try {
                    counter++;
                } finally {
                    locker.unlock(1);
                }
            } finally {
                locker.unlock(2);
            }
        } finally {
            locker.unlock(1);
        }
        assertEquals(1, counter);
        //assertEquals(0, locker.getLocksNumber());
    }

    @Test
    @SneakyThrows
    void timeout() {
        runAsync(() -> {
            locker.lock(1);
            try {
                sleep(3000);
            } finally {
                locker.unlock(1);
            }
        });

        CompletableFuture<Boolean> future = supplyAsync(() -> {
            try {
                sleep(25);
                return locker.tryLock(1, 10, MILLISECONDS);
            } catch (InterruptedException e) {
            }
            return true;
        });

        assertFalse(future.join());
    }

    @SneakyThrows
    private void sleep(int millis) {
        Thread.sleep(millis);
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
                    locker.unlock(1);
                }
            }
        }
    }

    @Test
    @SneakyThrows
    void entityLockedAndUnlocked_noTimeout() {
        runAsync(() -> {
            int id = 1;
            lock(id);
            unlock(id);
            sleep(5000);
        });

        int timeout = 500;
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
    }

    @Test
    @SneakyThrows
    void entityLockedWithTimeoutAndUnlocked_noTimeout() {
        int timeout = 500;
        runAsync(() -> {
            int id = 1;
            tryLock(id, timeout);
            unlock(id);
            sleep(200);
        });

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
    }

    @Test
    @SneakyThrows
    void entityLocked_timeout() {
        runAsync(() -> {
            int id = 1;
            lock(id);
            sleep(5000);
        });

        int timeout = 5;
        sleep(20);
        assertFalse(supplyAsync(() -> tryLock(1, timeout)).join());
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());
    }

    @Test
    @SneakyThrows
    void entityLockedWithTimeout_timeout() {
        int timeout = 50;
        runAsync(() -> {
            int id = 1;
            tryLock(id, timeout);
            sleep(5000);
        });

        sleep(20);
        assertFalse(supplyAsync(() -> tryLock(1, timeout)).join());
        assertTrue(supplyAsync(() -> {
            int id = 2;
            try {
                return tryLock(id, timeout);
            } finally {
                unlock(id);
            }
        }).join());
    }

    @SneakyThrows
    private void lock(int id) {
        locker.lock(id);
    }

    private void unlock(int id) {
        locker.unlock(id);
    }

    @SneakyThrows
    private boolean tryLock(int id, int timeout) {
        return locker.tryLock(id, timeout, MILLISECONDS);
    }
}
