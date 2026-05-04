package org.example.service;

import java.util.concurrent.atomic.AtomicInteger;

public class UserManager {
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    /**
     * Adds a user and increments the active user count.
     */
    public void addUser() {
        activeUsers.incrementAndGet();
    }

    /**
     * Removes a user and decrements the active user count.
     */
    public void removeUser() {
        activeUsers.decrementAndGet();
    }

    /**
     * Gets the current number of active users.
     *
     * @return number of active users
     */
    public int getActiveUserCount() {
        return activeUsers.get();
    }

    public void resetUsers() {
        activeUsers.set(0);
    }

}

