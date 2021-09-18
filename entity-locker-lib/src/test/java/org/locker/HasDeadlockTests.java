package org.locker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * t1 == thread1
 * r1 == resource1
 */
class HasDeadlockTests {

    private NoDeadLockEntityLocker<Integer> locker;

    @BeforeEach
    void setUp() {
        locker = new NoDeadLockEntityLocker<>(null);
    }

    @Test
    public void hasDeadlock_t1_has_r1_yet_pending_r2_and_t2_has_r2_yet_pending_r1_deadLock() {
        long thread1 = -1l;
        long thread2 = -2l;

        locker.addResource(1, thread1);
        locker.addResource(2, thread2);
        locker.addPendingResource(2, thread1);
        locker.addPendingResource(1, thread2);

        assertTrue(locker.hasDeadlock(1));
        assertTrue(locker.hasDeadlock(2));
    }

    @Test
    public void hasDeadlock_t1_has_r1_and_t2_pending_r1_noDeadLock() {
        long thread1 = -1l;
        long thread2 = -2l;

        locker.addResource(1, thread1);
        locker.addPendingResource(1, thread2);

        assertFalse(locker.hasDeadlock(1));
        Assertions.assertThrows(NullPointerException.class, () -> {
            locker.hasDeadlock(2);
        });
    }

    @Test
    public void hasDeadlock_t1_has_r1_yet_pending_r1_noDeadLock() {
        long thread1 = Thread.currentThread().getId();

        locker.addResource(1, thread1);
        locker.addPendingResource(1, thread1);

        assertFalse(locker.hasDeadlock(1));
    }

    @Test
    public void hasDeadlock_t1_has_r1_yet_pending_r2_and_t2_has_r2_noDeadLock() {
        long thread1 = -1l;
        long thread2 = -2l;

        locker.addResource(1, thread1);
        locker.addResource(2, thread2);
        locker.addPendingResource(2, thread1);

        assertFalse(locker.hasDeadlock(1));
        assertFalse(locker.hasDeadlock(2));
    }

    @Test
    public void hasDeadlock_t1_has_r1_and_t2_has_r2_noDeadLock() {
        long thread1 = -1l;
        long thread2 = -2l;

        locker.addResource(1, thread1);
        locker.addResource(2, thread2);

        assertFalse(locker.hasDeadlock(1));
        assertFalse(locker.hasDeadlock(2));
    }

    @Test
    public void hasDeadlock_t1_has_r1_noDeadLock() {
        long thread1 = -1l;

        locker.addResource(1, thread1);

        assertFalse(locker.hasDeadlock(1));
    }

    @Test
    public void hasDeadlock_free_r1_noDeadLock() {
        locker.addResource(1);

        assertFalse(locker.hasDeadlock(1));
    }

    @Test
    public void hasDeadlock_t1_has_r1_yet_pending_r2_and_t2_has_r2_yet_pending_r3_and_t3_has_r3_yet_pending_r1_deadLock() {
        long thread1 = -1l;
        long thread2 = -2l;
        long thread3 = -3l;

        locker.addResource(1, thread1);
        locker.addResource(2, thread2);
        locker.addResource(3, thread3);
        locker.addPendingResource(1, thread3);
        locker.addPendingResource(2, thread1);
        locker.addPendingResource(3, thread2);

        assertTrue(locker.hasDeadlock(1));
        assertTrue(locker.hasDeadlock(2));
        assertTrue(locker.hasDeadlock(3));
    }

    @Test
    public void hasDeadlock_t1_has_r1_yet_pending_r2_and_t2_has_r2_yet_pending_r3_and_t3_has_r3_noDeadLock() {
        long thread1 = -1l;
        long thread2 = -2l;
        long thread3 = -3l;

        locker.addResource(1, thread1);
        locker.addResource(2, thread2);
        locker.addResource(3, thread3);
        locker.addPendingResource(1, thread2);
        locker.addPendingResource(2, thread3);

        assertFalse(locker.hasDeadlock(1));
        assertFalse(locker.hasDeadlock(2));
        assertFalse(locker.hasDeadlock(3));
    }
}
