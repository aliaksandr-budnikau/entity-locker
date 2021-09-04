package org.locker;

interface EntityLocker<ID> {

    void lock(ID id);

    void unlock(ID id);
}
