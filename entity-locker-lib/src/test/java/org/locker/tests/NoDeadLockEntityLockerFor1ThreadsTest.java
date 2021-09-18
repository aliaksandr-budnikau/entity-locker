package org.locker.tests;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locker.EscalationSyncEntityLocker;
import org.locker.GlobalSyncEntityLocker;
import org.locker.NoDeadLockEntityLocker;
import org.locker.SyncEntityLocker;

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
        NoDeadLockEntityLocker<Integer> locker = new NoDeadLockEntityLocker<>(new EscalationSyncEntityLocker<>(new GlobalSyncEntityLocker<>(new SyncEntityLocker<>(null)), 100));
        locker.lock(1);
        try {
            locker.lock(1);
            locker.unlock(1);
        } finally {
            locker.unlock(1);
        }
    }

}
