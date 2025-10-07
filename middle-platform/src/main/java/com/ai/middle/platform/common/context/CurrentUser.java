package com.ai.middle.platform.common.context;

/**
 * Represents the authenticated user stored in the request context.
 */
public record CurrentUser(Long id, String username, String role) {
}
