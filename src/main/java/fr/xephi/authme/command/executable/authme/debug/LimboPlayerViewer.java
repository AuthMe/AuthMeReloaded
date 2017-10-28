package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.data.limbo.persistence.LimboPersistence;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.formatLocation;
import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.applyToLimboPlayersMap;

/**
 * Shows the data stored in LimboPlayers and the equivalent properties on online players.
 */
class LimboPlayerViewer implements DebugSection {

    @Inject
    private LimboService limboService;

    @Inject
    private LimboPersistence limboPersistence;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PermissionsManager permissionsManager;

    @Override
    public String getName() {
        return "limbo";
    }

    @Override
    public String getDescription() {
        return "View LimboPlayers and player's \"limbo stats\"";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        if (arguments.isEmpty()) {
            sender.sendMessage(ChatColor.BLUE + "AuthMe limbo viewer");
            sender.sendMessage("/authme debug limbo <player>: show a player's limbo info");
            sender.sendMessage("Available limbo records: " + applyToLimboPlayersMap(limboService, Map::keySet));
            return;
        }

        LimboPlayer memoryLimbo = limboService.getLimboPlayer(arguments.get(0));
        Player player = bukkitService.getPlayerExact(arguments.get(0));
        LimboPlayer diskLimbo = player != null ? limboPersistence.getLimboPlayer(player) : null;
        if (memoryLimbo == null && player == null) {
            sender.sendMessage(ChatColor.BLUE + "No AuthMe limbo data");
            sender.sendMessage("No limbo data and no player online with name '" + arguments.get(0) + "'");
            return;
        }

        sender.sendMessage(ChatColor.BLUE + "Player / limbo / disk limbo info for '" + arguments.get(0) + "'");
        new InfoDisplayer(sender, player, memoryLimbo, diskLimbo)
            .sendEntry("Is op", Player::isOp, LimboPlayer::isOperator)
            .sendEntry("Walk speed", Player::getWalkSpeed, LimboPlayer::getWalkSpeed)
            .sendEntry("Can fly", Player::getAllowFlight, LimboPlayer::isCanFly)
            .sendEntry("Fly speed", Player::getFlySpeed, LimboPlayer::getFlySpeed)
            .sendEntry("Location", p -> formatLocation(p.getLocation()), l -> formatLocation(l.getLocation()))
            .sendEntry("Prim. group",
                p -> permissionsManager.hasGroupSupport() ? permissionsManager.getPrimaryGroup(p) : "N/A",
                LimboPlayer::getGroups);
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.LIMBO_PLAYER_VIEWER;
    }

    /**
     * Displays the info for the given LimboPlayer and Player to the provided CommandSender.
     */
    private static final class InfoDisplayer {
        private final CommandSender sender;
        private final Optional<Player> player;
        private final Optional<LimboPlayer> memoryLimbo;
        private final Optional<LimboPlayer> diskLimbo;

        /**
         * Constructor.
         *
         * @param sender command sender to send the information to
         * @param player the player to get data from
         * @param memoryLimbo the limbo player to get data from
         */
        InfoDisplayer(CommandSender sender, Player player, LimboPlayer memoryLimbo, LimboPlayer diskLimbo) {
            this.sender = sender;
            this.player = Optional.ofNullable(player);
            this.memoryLimbo = Optional.ofNullable(memoryLimbo);
            this.diskLimbo = Optional.ofNullable(diskLimbo);

            if (memoryLimbo == null) {
                sender.sendMessage("Note: no Limbo information available");
            }
            if (player == null) {
                sender.sendMessage("Note: player is not online");
            } else if (diskLimbo == null) {
                sender.sendMessage("Note: no Limbo on disk available");
            }
        }

        /**
         * Displays a piece of information to the command sender.
         *
         * @param title the designation of the piece of information
         * @param playerGetter getter for data retrieval on Player
         * @param limboGetter getter for data retrieval on the LimboPlayer
         * @param <T> the data type
         * @return this instance (for chaining)
         */
        <T> InfoDisplayer sendEntry(String title,
                                    Function<Player, T> playerGetter,
                                    Function<LimboPlayer, T> limboGetter) {
            sender.sendMessage(
                title + ": "
                + getData(player, playerGetter)
                + " / "
                + getData(memoryLimbo, limboGetter)
                + " / "
                + getData(diskLimbo, limboGetter));
            return this;
        }

        static <E, T> String getData(Optional<E> entity, Function<E, T> getter) {
            return entity.map(getter).map(String::valueOf).orElse(" -- ");
        }
    }
}
