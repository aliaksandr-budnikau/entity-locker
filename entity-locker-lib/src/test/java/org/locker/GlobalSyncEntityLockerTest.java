package org.locker;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSyncEntityLockerTest {

    private GlobalSyncEntityLocker<Integer> locker;
    private int counter;

    @BeforeEach
    void setUp() {
        counter = 0;
        locker = new GlobalSyncEntityLocker<>(new SyncEntityLocker<>(null));
    }

    @Test
    @SneakyThrows
    void globallyLockedAndUnlocked_noTimeout() {
        runAsync(() -> {
            lock();
            unlock();
            sleep(1000);
        });

        int timeout = 500;
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
    }

    @Test
    @SneakyThrows
    void globallyLockedWithTimeoutAndUnlocked_noTimeout() {
        int timeout = 500;
        runAsync(() -> {
            tryLock(timeout);
            unlock();
            sleep(1000);
        });

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
    }

    @Test
    @SneakyThrows
    void globallyLocked_timeout() {
        runAsync(() -> {
            lock();
            sleep(1000);
        });

        int timeout = 5;
        sleep(20);
        assertFalse(supplyAsync(() -> tryLock(timeout)).join());
        assertFalse(supplyAsync(() -> tryLock(2, timeout)).join());
    }

    @Test
    @SneakyThrows
    void globallyLockedWithTimeout_timeout() {
        int timeout = 5;
        runAsync(() -> {
            tryLock(timeout);
            sleep(1000);
        });

        sleep(20);
        assertFalse(supplyAsync(() -> tryLock(timeout)).join());
        assertFalse(supplyAsync(() -> tryLock(2, timeout)).join());
    }

    @Test
    @SneakyThrows
    void entityLockedAndUnlocked_noTimeout() {
        runAsync(() -> {
            int id = 1;
            lock(id);
            unlock(id);
            sleep(1000);
        });

        int timeout = 500;
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
    }

    @Test
    @SneakyThrows
    void entityLockedWithTimeoutAndUnlocked_noTimeout() {
        int timeout = 500;
        runAsync(() -> {
            int id = 1;
            tryLock(id, timeout);
            unlock(id);
            sleep(1000);
        });

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
    }

    @Test
    @SneakyThrows
    void entityLocked_timeout() {
        runAsync(() -> {
            int id = 1;
            lock(id);
            sleep(1000);
        });

        int timeout = 5;
        sleep(20);
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
    }

    @Test
    @SneakyThrows
    void entityLockedWithTimeout_timeout() {
        int timeout = 50;
        runAsync(() -> {
            int id = 1;
            tryLock(id, timeout);
            sleep(1000);
        });

        sleep(20);
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
    private void lock() {
        locker.lock();
    }

    @SneakyThrows
    private void lock(int id) {
        locker.lock(id);
    }

    @SneakyThrows
    private void sleep(int millis) {
        Thread.sleep(millis);
    }

    @SneakyThrows
    private void doIncrements(int endExclusive) {
        int i = endExclusive;
        while (i-- != 0) {
            locker.lock();
            try {
                counter++;
            } finally {
                unlock();
            }
        }
    }

    private void unlock(int id) {
        locker.unlock(id);
    }

    @SneakyThrows
    private boolean tryLock(int id, int timeout) {
        return locker.tryLock(id, timeout, MILLISECONDS);
    }

    @SneakyThrows
    private boolean tryLock(int timeout) {
        return locker.tryLock(timeout, MILLISECONDS);
    }

    private void unlock() {
        locker.unlock();
    }
}

