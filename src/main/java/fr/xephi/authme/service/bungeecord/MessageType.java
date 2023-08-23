package fr.xephi.authme.service.bungeecord;

import java.util.Optional;

public enum MessageType {
    LOGIN("login", true),
    LOGOUT("logout", true),
    PERFORM_LOGIN("perform.login", false);

    private final String id;
    private final boolean broadcast;

    MessageType(String id, boolean broadcast) {
        this.id = id;
        this.broadcast = broadcast;
    }

    public String getId() {
        return id;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    /**
     * Returns the MessageType with the given ID.
     *
     * @param id the message type id.
     *
     * @return the MessageType with the given id, empty if invalid.
     */
    public static Optional<MessageType> fromId(String id) {
        for (MessageType current : values()) {
            if (current.getId().equals(id)) {
                return Optional.of(current);
            }
        }
        return Optional.empty();
    }

}
