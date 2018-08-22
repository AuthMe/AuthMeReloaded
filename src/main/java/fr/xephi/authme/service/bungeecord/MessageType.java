package fr.xephi.authme.service.bungeecord;

public enum MessageType {
    // Placeholders
    UNKNOWN("unknown", false),
    // Broadcast messages
    REFRESH_PASSWORD("refresh.password", true),
    REFRESH_SESSION("refresh.session", true),
    REFRESH_QUITLOC("refresh.quitloc", true),
    REFRESH_EMAIL("refresh.email", true),
    REFRESH("refresh", true),
    REGISTER("register", true),
    UNREGISTER("unregister", true),
    // Bungee-only outgoing messages
    // TODO #1614: should be broadcasts, AuthMeBungee needs to be adapted.
    LOGIN("login", false),
    LOGOUT("logout", false),
    // Bungee-only incoming messages
    BUNGEE_LOGIN("bungeelogin", false);

    private final String id;
    private final boolean broadcast;
    private final boolean requiresCaching;

    MessageType(String id, boolean broadcast, boolean requiresCaching) {
        this.id = id;
        this.broadcast = broadcast;
        this.requiresCaching = requiresCaching;
    }

    MessageType(String id, boolean broadcast) {
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
     * @return the MessageType with the given id, MessageType.UNKNOWN if invalid.
     */
    public static MessageType fromId(String id) {
        for (MessageType current : values()) {
            if (current.getId().equals(id)) {
                return current;
            }
        }
        return UNKNOWN;
    }
}
