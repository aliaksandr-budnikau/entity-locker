package org.locker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class EscalationEntityLocker<ID> implements EntityLocker<ID> {

    private final int threshold;
    private final EntityLocker<ID> locker;
    private final ReentrantLock escalatedLock = new ReentrantLock();
    private final ThreadLocal<Integer> locksCounter = new ThreadLocal<>();

    public EscalationEntityLocker(EntityLocker<ID> locker, int threshold) {
        this.locker = locker;
        this.threshold = threshold;
    }

    public void lock(ID id) {
        locker.lock(id);
        synchronized (this) {
            int counter = incrementAndGetCounter();
            if (counter > threshold) {
                escalatedLock.lock();
            }
        }
    }

    public boolean tryLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        if (!locker.tryLock(id, timeout, unit)) {
            return false;
        }
        synchronized (this) {
            int counter = incrementAndGetCounter();
            if (counter > threshold) {
                if (!escalatedLock.tryLock(timeout, unit)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void unlock(ID id) {
        synchronized (this) {
            int counter = decrementAndGetCounter();
            if (counter > threshold) {
                escalatedLock.unlock();
            }
        }
        locker.unlock(id);
    }

    private int incrementAndGetCounter() {
        Integer counter = locksCounter.get();
        if (counter == null) {
            locksCounter.set(1);
        } else {
            locksCounter.set(counter + 1);
        }
        return locksCounter.get();
    }

    private int decrementAndGetCounter() {
        Integer counter = locksCounter.get();
        locksCounter.set(counter - 1);
        return locksCounter.get();
    }
}
