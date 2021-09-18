package org.locker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class BasicEntityLocker<ID> implements EntityLocker<ID> {
    private final Map<ID, LockWrapper> locks;

    public BasicEntityLocker() {
        locks = new ConcurrentHashMap<>();
    }

    @Override
    public void lock(ID id) {
        try {
            this.tryLock(id, 0, null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean tryLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        return getLock(id, timeout, unit) != null;
    }

    @Override
    public void unlock(ID id) {
        locks.computeIfPresent(id, this::releaseLockAndRemoveIfNotUsed);
    }

    private ReentrantLock getLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        ReentrantLock lock = locks.compute(id, this::createLockWrapperOrGetOld).getLock();
        if (timeout >= 0 && unit != null) {
            if (!lock.tryLock(timeout, unit)) {
                return null;
            }
        } else {
            lock.lock();
        }
        return lock;
    }

    int getLocksNumber() {
        return locks.size();
    }

    private LockWrapper createLockWrapperOrGetOld(ID id, LockWrapper oldWrapper) {
        if (oldWrapper == null) {
            oldWrapper = new LockWrapper();
        }
        oldWrapper.incrementCounter();
        return oldWrapper;
    }

    private LockWrapper releaseLockAndRemoveIfNotUsed(ID id, LockWrapper wrapper) {
        wrapper.getLock().unlock();
        if (wrapper.decrementAndGetCounter() == 0) {
            return null;
        }
        return wrapper;
    }

    private static class LockWrapper {
        private final ReentrantLock lock;
        private int counter;

        private LockWrapper() {
            this.lock = new ReentrantLock();
        }

        public ReentrantLock getLock() {
            return lock;
        }

        public void incrementCounter() {
            counter++;
        }

        public int decrementAndGetCounter() {
            counter--;
            return counter;
        }
    }
}
