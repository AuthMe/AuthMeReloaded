package fr.xephi.authme.permission.handlers;

import java.util.UUID;

/**
 * Exception thrown when a {@link PermissionHandler#loadUserData(UUID uuid)} request fails.
 */
public class PermissionLoadUserException extends Exception {

    public PermissionLoadUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
