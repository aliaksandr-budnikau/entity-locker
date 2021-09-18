package org.locker.tests;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locker.BasicEntityLocker;
import org.locker.EntityLocker;
import org.locker.EscalationEntityLocker;
import org.locker.NoDeadLockEntityLocker;

import static java.lang.Math.random;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NoDeadLockEntityLockerTest {

    private static final int THRESHOLD = 3;
    private EntityLocker<Integer> locker;
    private volatile int counter;

    @BeforeEach
    void setUp() {
        counter = 0;
        locker = new EscalationEntityLocker<>(new NoDeadLockEntityLocker<>(new BasicEntityLocker<>()), THRESHOLD);
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
                double lockType = random() * 2;
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
}
