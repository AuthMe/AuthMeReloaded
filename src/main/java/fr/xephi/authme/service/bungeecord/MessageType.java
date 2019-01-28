package fr.xephi.authme.service.bungeecord;

import java.util.Optional;

public enum MessageType {
    REFRESH_PASSWORD("refresh.password", true, true),
    REFRESH_SESSION("refresh.session", true, true),
    REFRESH_EMAIL("refresh.email", true, true),
    REFRESH("refresh", true, true),
    REGISTER("register", true),
    UNREGISTER("unregister", true),
    LOGIN("login", true),
    LOGOUT("logout", true),
    PERFORM_LOGIN("perform.login", false);

    private final String id;
    private final boolean broadcast;
    private final boolean requiresCaching;

    MessageType(final String id, final boolean broadcast, final boolean requiresCaching) {
        this.id = id;
        this.broadcast = broadcast;
        this.requiresCaching = requiresCaching;
    }

    MessageType(final String id, final boolean broadcast) {
        this(id, broadcast, false);
    }

    public String getId() {
        return id;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public boolean isRequiresCaching() {
        return requiresCaching;
    }

    /**
     * Returns the MessageType with the given ID.
     *
     * @param id the message type id.
     *
     * @return the MessageType with the given id, empty if invalid.
     */
    public static Optional<MessageType> fromId(final String id) {
        for (final MessageType current : values()) {
            if (current.getId().equals(id)) {
                return Optional.of(current);
            }
        }
        return Optional.empty();
    }

}
