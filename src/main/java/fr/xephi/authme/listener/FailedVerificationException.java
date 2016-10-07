package fr.xephi.authme.listener;

import fr.xephi.authme.message.MessageKey;

/**
 * Exception thrown when a verification has failed.
 */
public class FailedVerificationException extends Exception {

    private static final long serialVersionUID = 3903242223297960699L;
    private final MessageKey reason;
    private final String[] args;

    public FailedVerificationException(MessageKey reason, String... args) {
        this.reason = reason;
        this.args = args;
    }

    public MessageKey getReason() {
        return reason;
    }

    public String[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": reason=" + reason
            + ";args=" + (args == null ? "null" : String.join(", ", args));
    }
}
