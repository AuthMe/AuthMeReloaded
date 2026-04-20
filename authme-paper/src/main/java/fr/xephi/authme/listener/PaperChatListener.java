package fr.xephi.authme.listener;

import io.papermc.paper.event.player.AsyncChatEvent;

/**
 * Chat listener for PaperMC that handles {@link AsyncChatEvent}
 * (replaces the deprecated {@code AsyncPlayerChatEvent} on Paper).
 * Mirrors the logic of {@code PlayerListener#onPlayerChat} for full feature parity,
 * including HIDE_CHAT recipient filtering via the Adventure API.
 */
public class PaperChatListener extends AbstractPaperAsyncChatListener {

    /**
     * Constructor.
     */
    public PaperChatListener() {
    }
}
