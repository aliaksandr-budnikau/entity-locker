package org.locker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class EscalationSyncEntityLocker<ID> {

    private final int threshold;
    private final GlobalSyncEntityLocker<ID> locker;
    private final ReentrantLock escalatedLock = new ReentrantLock();
    private volatile int locksCounter = 0;

    public EscalationSyncEntityLocker(GlobalSyncEntityLocker<ID> locker, int threshold) {
        this.locker = locker;
        this.threshold = threshold;
    }

    public void lock(ID id) throws InterruptedException {
        locker.lock(id);
        synchronized (this) {
            locksCounter++;
            if (locksCounter > threshold) {
                escalatedLock.lock();
            }
        }
    }

    public boolean tryLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        if (!locker.tryLock(id, timeout, unit)) {
            return false;
        }
        synchronized (this) {
            locksCounter++;
            if (locksCounter > threshold) {
                if (!escalatedLock.tryLock(timeout, unit)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void unlock(ID id) {
        synchronized (this) {
            locksCounter--;
            if (locksCounter > threshold) {
                escalatedLock.unlock();
            }
        }
        locker.unlock(id);
    }

    public void lock() throws InterruptedException {
        locker.lock();
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return locker.tryLock(timeout, unit);
    }

    public void unlock() {
        locker.unlock();
    }

}
