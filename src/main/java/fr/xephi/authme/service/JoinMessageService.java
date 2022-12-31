package fr.xephi.authme.service;

import fr.xephi.authme.util.StringUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The JoinMessageService class.
 */
public class JoinMessageService {

    private BukkitService bukkitService;

    private Map<String, String> joinMessages;

    @Inject
    JoinMessageService(BukkitService bukkitService) {
        this.bukkitService = bukkitService;
        joinMessages = new ConcurrentHashMap<>();
    }

    /**
     * Store a join message.
     *
     * @param playerName the player name
     * @param string     the join message
     */
    public void putMessage(String playerName, String string) {
        joinMessages.put(playerName, string);
    }

    /**
     * Broadcast the join message of the specified player.
     *
     * @param playerName the player name
     */
    public void sendMessage(String playerName) {
        String joinMessage = joinMessages.remove(playerName);
        if (!StringUtils.isBlank(joinMessage)) {
            bukkitService.broadcastMessage(joinMessage);
        }
    }
}
