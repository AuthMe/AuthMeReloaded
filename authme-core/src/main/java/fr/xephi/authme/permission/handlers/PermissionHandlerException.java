package fr.xephi.authme.permission.handlers;

/**
 * Exception during the instantiation of a {@link PermissionHandler}.
 */
@SuppressWarnings("serial")
public class PermissionHandlerException extends Exception {

    public PermissionHandlerException(String message) {
        super(message);
    }

    public PermissionHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
