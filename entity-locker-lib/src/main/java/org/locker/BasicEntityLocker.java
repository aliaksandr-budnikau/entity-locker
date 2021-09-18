package org.locker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class BasicEntityLocker<ID> implements EntityLocker<ID> {
    private final Map<ID, ReentrantLock> locks;

    public BasicEntityLocker() {
        locks = new ConcurrentHashMap<>();
    }

    @Override
    public void lock(ID id) {
        try {
            this.tryLock(id, 0, null);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public boolean tryLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        ReentrantLock lock = getLock(id, timeout, unit);
        return lock != null;
    }

    @Override
    public void unlock(ID id) {
        locks.get(id).unlock();
    }

    private ReentrantLock getLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        ReentrantLock lock = locks.computeIfAbsent(id, key -> new ReentrantLock());
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
}
