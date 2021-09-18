package org.locker.tests;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locker.BasicEntityLocker;
import org.locker.EntityLocker;
import org.locker.EscalationEntityLocker;
import org.locker.NoDeadLockEntityLocker;

/**
 * t1 == thread1
 * r1 == resource1
 */
class NoDeadLockEntityLockerFor1ThreadsTest {

    @Test
    public void reproduceDeadlock_t1_has_r1_yet_pending_r1() {
        for (int i = 0; i < 100000; i++) {
            reproduceDeadlock();
        }
    }

    @SneakyThrows
    private void reproduceDeadlock() {
        EntityLocker<Integer> locker = new NoDeadLockEntityLocker<>(new EscalationEntityLocker<>(new BasicEntityLocker<>(), 100));
        locker.lock(1);
        try {
            locker.lock(1);
            locker.unlock(1);
        } finally {
            locker.unlock(1);
        }
    }

}
