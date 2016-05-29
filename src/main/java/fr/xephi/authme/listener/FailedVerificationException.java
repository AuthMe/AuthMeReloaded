package fr.xephi.authme.listener;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.StringUtils;

/**
 * Exception thrown when a verification has failed.
 */
public class FailedVerificationException extends Exception {

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
        return getClass().getSimpleName() + ": reason=" + (reason == null ? "null" : reason)
            + ";args=" + (args == null ? "null" : StringUtils.join(", ", args));
    }
}
