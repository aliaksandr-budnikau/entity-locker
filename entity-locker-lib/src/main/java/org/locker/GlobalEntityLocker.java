package org.locker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public final class GlobalEntityLocker<ID> {
    private final EntityLocker<ID> locker;
    private final ReentrantLock globalLock = new ReentrantLock();
    private final AtomicInteger entityLocksCounter = new AtomicInteger();

    public GlobalEntityLocker(EntityLocker<ID> locker) {
        this.locker = locker;
    }

    public void lock(ID id) throws InterruptedException {
        awaitGlobalLock();
        locker.lock(id);
        entityLocksCounter.incrementAndGet();
    }

    public boolean tryLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        if (awaitGlobalLock(timeout, unit)) {
            boolean result = locker.tryLock(id, timeout, unit);
            if (result) {
                entityLocksCounter.incrementAndGet();
            }
            return result;
        }
        return false;
    }

    public void unlock(ID id) {
        locker.unlock(id);
        entityLocksCounter.decrementAndGet();
    }

    public void lock() throws InterruptedException {
        awaitEntityLock();
        globalLock.lock();
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        if (awaitEntityLock(timeout, unit)) {
            return globalLock.tryLock(timeout, unit);
        }
        return false;
    }

    public void unlock() {
        globalLock.unlock();
        synchronized (globalLock) {
            globalLock.notifyAll();
        }
    }

    private void awaitGlobalLock() throws InterruptedException {
        synchronized (globalLock) {
            while (globalLock.isLocked()) {
                globalLock.wait();
            }
        }
    }

    private boolean awaitGlobalLock(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (globalLock) {
            long startTime = System.currentTimeMillis();
            while (globalLock.isLocked()) {
                unit.timedWait(globalLock, timeout);
                long currentTime = System.currentTimeMillis();
                if ((currentTime - startTime) >= unit.toMillis(timeout)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void awaitEntityLock() throws InterruptedException {
        synchronized (entityLocksCounter) {
            while (entityLocksCounter.get() != 0) {
                entityLocksCounter.wait();
            }
        }
    }

    private boolean awaitEntityLock(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (entityLocksCounter) {
            long startTime = System.currentTimeMillis();
            while (entityLocksCounter.get() != 0) {
                unit.timedWait(entityLocksCounter, timeout);
                long currentTime = System.currentTimeMillis();
                if ((currentTime - startTime) >= unit.toMillis(timeout)) {
                    return false;
                }
            }
        }
        return true;
    }
}
