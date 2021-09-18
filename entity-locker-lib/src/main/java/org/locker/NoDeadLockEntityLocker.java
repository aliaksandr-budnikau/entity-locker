package org.locker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class NoDeadLockEntityLocker<ID> {
    private final Object lockObject = new Object();
    private final Map<ID, Resource<ID>> resourceId2resourceMap;
    private final Map<Long, Resource<ID>> threadId2PendingResourceMap;
    private final EscalationSyncEntityLocker<ID> locker;

    public NoDeadLockEntityLocker(EscalationSyncEntityLocker<ID> locker) {
        this.locker = locker;
        resourceId2resourceMap = new HashMap<>();
        threadId2PendingResourceMap = new HashMap<>();
    }

    public void lock(ID id) throws InterruptedException {
        synchronized (lockObject) {
            if (resourceId2resourceMap.containsKey(id)) {
                addPendingResource(id, getThreadId());
                if (hasDeadlock(id)) {
                    removePendingResource();
                    throw new DeadlockDetectedException(format("Deadlock detected. Thread id %s could not lock %s.", getThreadId(), id));
                }
            }
        }
        locker.lock(id);
        synchronized (lockObject) {
            removePendingResource();
            addResource(id, getThreadId());
        }
    }

    public boolean tryLock(ID id, long timeout, TimeUnit unit) throws InterruptedException {
        return locker.tryLock(id, timeout, unit);
    }

    public void unlock(ID id) {
        synchronized (lockObject) {
            removeResource(id);
            locker.unlock(id);
        }
    }

    void addResource(ID id, long ownerThreadId) {
        resourceId2resourceMap.put(id, new Resource<>(id, ownerThreadId));
    }

    void addResource(ID id) {
        resourceId2resourceMap.put(id, new Resource<>(id));
    }

    void addPendingResource(ID id, long pendingThreadId) {
        threadId2PendingResourceMap.put(pendingThreadId, resourceId2resourceMap.get(id));
    }

    boolean hasDeadlock(ID id) {
        Set<Resource<ID>> visitedResources = new HashSet<>();
        return hasNextPendingResource(id, visitedResources);
    }

    private void removeResource(ID id) {
        resourceId2resourceMap.remove(id);
    }

    private void removePendingResource() {
        threadId2PendingResourceMap.remove(getThreadId());
    }

    private boolean hasNextPendingResource(ID neededResourceId, Set<Resource<ID>> visitedResources) {
        Resource<ID> neededResource = resourceId2resourceMap.get(neededResourceId);
        if (visitedResources.contains(neededResource)) {
            return neededResource.getOwner() != getThreadId();
        }

        Long neededResourceOwnerId = neededResource.getOwner();
        if (neededResourceOwnerId == null) {
            return false;
        }

        Resource<ID> pendingResource = threadId2PendingResourceMap.get(neededResourceOwnerId);
        if (pendingResource == null) {
            return false;
        }

        visitedResources.add(neededResource);
        return hasNextPendingResource(pendingResource.getId(), visitedResources);
    }

    private long getThreadId() {
        return Thread.currentThread().getId();
    }

    private static class Resource<ID> {
        private final ID id;
        private Long owner;

        public Resource(ID id, Long owner) {
            this.id = id;
            this.owner = owner;
        }

        public Resource(ID id) {
            this.id = id;
        }

        public ID getId() {
            return id;
        }

        public Long getOwner() {
            return owner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Resource<?> resource = (Resource<?>) o;
            return id.equals(resource.id) && owner.equals(resource.owner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, owner);
        }
    }
}
