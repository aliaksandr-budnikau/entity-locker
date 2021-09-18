package org.locker;

public class DeadlockDetectedException extends RuntimeException {
    public DeadlockDetectedException(String message) {
        super(message);
    }
}
