package org.locker;

import java.util.concurrent.TimeUnit;

public interface EntityLocker<ID> {

    void lock(ID id);

    boolean tryLock(ID id, long timeout, TimeUnit unit) throws InterruptedException;

    void unlock(ID id);
}
