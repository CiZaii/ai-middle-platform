package com.ai.middle.platform.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.springframework.util.Assert;

/**
 * Stores authenticated user info in a TransmittableThreadLocal so downstream components
 * (including async tasks) can access request scoped user data.
 */
public final class UserContextHolder {

    private static final TransmittableThreadLocal<CurrentUser> CONTEXT = new TransmittableThreadLocal<>();

    private UserContextHolder() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void set(CurrentUser currentUser) {
        Assert.notNull(currentUser, "currentUser must not be null");
        CONTEXT.set(currentUser);
    }

    public static CurrentUser get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        CurrentUser currentUser = CONTEXT.get();
        return currentUser != null ? currentUser.id() : null;
    }

    public static String getUsername() {
        CurrentUser currentUser = CONTEXT.get();
        return currentUser != null ? currentUser.username() : null;
    }

    public static String getRole() {
        CurrentUser currentUser = CONTEXT.get();
        return currentUser != null ? currentUser.role() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
