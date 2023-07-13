package org.example;

import java.util.concurrent.Semaphore;

public class ResourceLock {
    private Semaphore semaphore;

    public ResourceLock(int permits) {
        semaphore = new Semaphore(permits);
    }

    public boolean acquire() {
        return semaphore.tryAcquire();
    }

    public void release() {
        semaphore.release();
    }
}
