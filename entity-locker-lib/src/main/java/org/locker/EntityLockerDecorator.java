package org.locker;

abstract class EntityLockerDecorator<ID> implements EntityLocker<ID> {

    private final EntityLocker<ID> locker;

    public EntityLockerDecorator(EntityLocker<ID> locker) {
        this.locker = locker;
    }

    @Override
    public void lock(ID id) {
        locker.lock(id);
    }

    @Override
    public void unlock(ID id) {
        locker.unlock(id);
    }
}

