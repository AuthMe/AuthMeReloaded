package fr.xephi.authme.commands;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.GroupType;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AdminCommand implements CommandExecutor {

    public AuthMe plugin;
    private Messages m = Messages.getInstance();

    public AdminCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmnd,
                             String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage:");
            sender.sendMessage("/authme reload - Reload the config");
            sender.sendMessage("/authme version - Get AuthMe version info");
            sender.sendMessage("/authme register <playername> <password> - Register a player");
            sender.sendMessage("/authme unregister <playername> - Unregister a player");
            sender.sendMessage("/authme changepassword <playername> <password> - Change a player's password");
            sender.sendMessage("/authme chgemail <playername> <email> - Change a player's email");
            sender.sendMessage("/authme getemail <playername> - Get a player's email");
            sender.sendMessage("/authme getip <onlineplayername> - Display a player's IP if he's online");
            sender.sendMessage("/authme lastlogin <playername> - Display the date of a player's last login");
            sender.sendMessage("/authme accounts <playername> - Display all player's accounts");
            sender.sendMessage("/authme purge <days> - Purge database");
            sender.sendMessage("/authme purgebannedplayers - Purge database from banned players");
            sender.sendMessage("/authme purgelastpos <playername> - Purge last position infos for a player");
            sender.sendMessage("/authme setspawn - Set player's spawn to your current position");
            sender.sendMessage("/authme setfirstspawn - Set player's first spawn to your current position");
            sender.sendMessage("/authme spawn - Teleport yourself to the spawn point");
            sender.sendMessage("/authme firstspawn - Teleport yourself to the first spawn point");
            sender.sendMessage("/authme switchantibot on/off - Enable/Disable AntiBot feature");
            sender.sendMessage("/authme forcelogin <playername> - Enforce the login of a connected player");
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme.admin." + args[0].toLowerCase())) {
            m.send(sender, "no_perm");
            return true;
        }

        if (args[0].equalsIgnoreCase("version")) {
            sender.sendMessage("AuthMe Version: " + AuthMe.getInstance().getDescription().getVersion());
            return true;
        }

        if (args[0].equalsIgnoreCase("purge")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /authme purge <days>");
                return true;
            }
            if (Integer.parseInt(args[1]) < 30) {
                sender.sendMessage("You can only purge data older than 30 days");
                return true;
            }
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -(Integer.parseInt(args[1])));
                long until = calendar.getTimeInMillis();
                List<String> purged = plugin.database.autoPurgeDatabase(until);
                sender.sendMessage("Deleted " + purged.size() + " user accounts");
                if (Settings.purgeEssentialsFile && plugin.ess != null)
                    plugin.dataManager.purgeEssentials(purged);
                if (Settings.purgePlayerDat)
                    plugin.dataManager.purgeDat(purged);
                if (Settings.purgeLimitedCreative)
                    plugin.dataManager.purgeLimitedCreative(purged);
                if (Settings.purgeAntiXray)
                    plugin.dataManager.purgeAntiXray(purged);
                sender.sendMessage("[AuthMe] Database has been purged correctly");
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("Usage: /authme purge <days>");
                return true;
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.getSettings().reload();
                m.reloadMessages();
                plugin.database.close();
                plugin.setupDatabase();
            } catch (Exception e) {
                ConsoleLogger.showError("Fatal error occurred! Authme instance ABORTED!");
                plugin.stopOrUnload();
                return false;
            }
            m.send(sender, "reload");
        } else if (args[0].equalsIgnoreCase("lastlogin")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /authme lastlogin <playername>");
                return true;
            }
            PlayerAuth auth;
            try {
                auth = plugin.database.getAuth(args[1].toLowerCase());
            } catch (NullPointerException e) {
                m.send(sender, "unknown_user");
                return true;
            }
            if (auth == null) {
                m.send(sender, "user_unknown");
                return true;
            }
            long lastLogin = auth.getLastLogin();
            Date d = new Date(lastLogin);
            final long diff = System.currentTimeMillis() - lastLogin;
            final String msg = (int) (diff / 86400000) + " days " + (int) (diff / 3600000 % 24) + " hours " + (int) (diff / 60000 % 60) + " mins " + (int) (diff / 1000 % 60) + " secs.";
            String lastIP = auth.getIp();
            sender.sendMessage("[AuthMe] " + args[1] + " lastlogin : " + d.toString());
            sender.sendMessage("[AuthMe] The player " + auth.getNickname() + " is unlogged since " + msg);
            sender.sendMessage("[AuthMe] Last Player's IP: " + lastIP);
        } else if (args[0].equalsIgnoreCase("accounts")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /authme accounts <playername>");
                sender.sendMessage("Or: /authme accounts <ip>");
                return true;
            }
            if (!args[1].contains(".")) {
                final String[] arguments = args;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        PlayerAuth auth;
                        StringBuilder message = new StringBuilder("[AuthMe] ");
                        try {
                            auth = plugin.database.getAuth(arguments[1].toLowerCase());
                        } catch (NullPointerException npe) {
                            m.send(sender, "unknown_user");
                            return;
                        }
                        if (auth == null) {
                            m.send(sender, "unknown_user");
                            return;
                        }
                        List<String> accountList = plugin.database.getAllAuthsByName(auth);
                        if (accountList == null || accountList.isEmpty()) {
                            m.send(sender, "user_unknown");
                            return;
                        }
                        if (accountList.size() == 1) {
                            sender.sendMessage("[AuthMe] " + arguments[1] + " is a single account player");
                            return;
                        }
                        int i = 0;
                        for (String account : accountList) {
                            i++;
                            message.append(account);
                            if (i != accountList.size()) {
                                message.append(", ");
                            } else {
                                message.append(".");
                            }
                        }
                        sender.sendMessage("[AuthMe] " + arguments[1] + " has " + String.valueOf(accountList.size()) + " accounts");
                        sender.sendMessage(message.toString());
                    }
                });
                return true;
            } else {
                final String[] arguments = args;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder message = new StringBuilder("[AuthMe] ");
                        if (arguments[1] == null) {
                            sender.sendMessage("[AuthMe] Please put a valid IP");
                            return;
                        }
                        List<String> accountList = plugin.database.getAllAuthsByIp(arguments[1]);
                        if (accountList == null || accountList.isEmpty()) {
                            sender.sendMessage("[AuthMe] This IP does not exist in the database");
                            return;
                        }
                        if (accountList.size() == 1) {
                            sender.sendMessage("[AuthMe] " + arguments[1] + " is a single account player");
                            return;
                        }
                        int i = 0;
                        for (String account : accountList) {
                            i++;
                            message.append(account);
                            if (i != accountList.size()) {
                                message.append(", ");
                            } else {
                                message.append(".");
                            }
                        }
                        sender.sendMessage("[AuthMe] " + arguments[1] + " has " + String.valueOf(accountList.size()) + " accounts");
                        sender.sendMessage(message.toString());
                    }
                });
                return true;
            }
        } else if (args[0].equalsIgnoreCase("register") || args[0].equalsIgnoreCase("reg")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /authme register <playername> <password>");
                return true;
            }
            final String name = args[1].toLowerCase();
            final String lowpass = args[2].toLowerCase();
            if (lowpass.contains("delete") || lowpass.contains("where") || lowpass.contains("insert") || lowpass.contains("modify") || lowpass.contains("from") || lowpass.contains("select") || lowpass.contains(";") || lowpass.contains("null") || !lowpass.matches(Settings.getPassRegex)) {
                m.send(sender, "password_error");
                return true;
            }
            if (lowpass.equalsIgnoreCase(args[1])) {
                m.send(sender, "password_error_nick");
                return true;
            }
            if (lowpass.length() < Settings.getPasswordMinLen || lowpass.length() > Settings.passwordMaxLength) {
                m.send(sender, "pass_len");
                return true;
            }
            if (!Settings.unsafePasswords.isEmpty()) {
                if (Settings.unsafePasswords.contains(lowpass)) {
                    m.send(sender, "password_error_unsafe");
                    return true;
                }
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (plugin.database.isAuthAvailable(name)) {
                            m.send(sender, "user_regged");
                            return;
                        }
                        String hash = PasswordSecurity.getHash(Settings.getPasswordHash, lowpass, name);
                        PlayerAuth auth = new PlayerAuth(name, hash, "192.168.0.1", 0L, "your@email.com");
                        if (PasswordSecurity.userSalt.containsKey(name) && PasswordSecurity.userSalt.get(name) != null)
                            auth.setSalt(PasswordSecurity.userSalt.get(name));
                        else auth.setSalt("");
                        if (!plugin.database.saveAuth(auth)) {
                            m.send(sender, "error");
                            return;
                        }
                        m.send(sender, "registered");
                        ConsoleLogger.info(name + " registered");
                    } catch (NoSuchAlgorithmException ex) {
                        ConsoleLogger.showError(ex.getMessage());
                        m.send(sender, "error");
                    }

                }
            });
            return true;
        } else if (args[0].equalsIgnoreCase("getemail")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /authme getemail <playername>");
                return true;
            }
            String playername = args[1].toLowerCase();
            PlayerAuth auth = plugin.database.getAuth(playername);
            if (auth == null) {
                m.send(sender, "unknown_user");
                return true;
            }
            sender.sendMessage("[AuthMe] " + args[1] + "'s email: " + auth.getEmail());
            return true;
        } else if (args[0].equalsIgnoreCase("chgemail")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /authme chgemail <playername> <email>");
                return true;
            }
            if (!Settings.isEmailCorrect(args[2])) {
                m.send(sender, "email_invalid");
                return true;
            }
            String playername = args[1].toLowerCase();
            PlayerAuth auth = plugin.database.getAuth(playername);
            if (auth == null) {
                m.send(sender, "unknown_user");
                return true;
            }
            auth.setEmail(args[2]);
            if (!plugin.database.updateEmail(auth)) {
                m.send(sender, "error");
                return true;
            }
            if (PlayerCache.getInstance().getAuth(playername) != null)
                PlayerCache.getInstance().updatePlayer(auth);
            m.send(sender, "email_changed");
            return true;
        } else if (args[0].equalsIgnoreCase("setspawn")) {
            try {
                if (sender instanceof Player) {
                    if (Spawn.getInstance().setSpawn(((Player) sender).getLocation())) {
                        sender.sendMessage("[AuthMe] Correctly defined new spawn point");
                    } else {
                        sender.sendMessage("[AuthMe] SetSpawn has failed, please retry");
                    }
                } else {
                    sender.sendMessage("[AuthMe] Please use that command in game");
                }
            } catch (NullPointerException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
            return true;
        } else if (args[0].equalsIgnoreCase("setfirstspawn")) {
            try {
                if (sender instanceof Player) {
                    if (Spawn.getInstance().setFirstSpawn(((Player) sender).getLocation()))
                        sender.sendMessage("[AuthMe] Correctly defined new first spawn point");
                    else sender.sendMessage("[AuthMe] SetFirstSpawn has failed, please retry");
                } else {
                    sender.sendMessage("[AuthMe] Please use that command in game");
                }
            } catch (NullPointerException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
            return true;
        } else if (args[0].equalsIgnoreCase("purgebannedplayers")) {
            List<String> bannedPlayers = new ArrayList<>();
            for (OfflinePlayer off : plugin.getServer().getBannedPlayers()) {
                bannedPlayers.add(off.getName().toLowerCase());
            }
            plugin.database.purgeBanned(bannedPlayers);
            if (Settings.purgeEssentialsFile && plugin.ess != null)
                plugin.dataManager.purgeEssentials(bannedPlayers);
            if (Settings.purgePlayerDat)
                plugin.dataManager.purgeDat(bannedPlayers);
            if (Settings.purgeLimitedCreative)
                plugin.dataManager.purgeLimitedCreative(bannedPlayers);
            if (Settings.purgeAntiXray)
                plugin.dataManager.purgeAntiXray(bannedPlayers);
            sender.sendMessage("[AuthMe] Database has been purged correctly");
            return true;
        } else if (args[0].equalsIgnoreCase("spawn")) {
            try {
                if (sender instanceof Player) {
                    if (Spawn.getInstance().getSpawn() != null)
                        ((Player) sender).teleport(Spawn.getInstance().getSpawn());
                    else sender.sendMessage("[AuthMe] Spawn has failed, please try to define the spawn");
                } else {
                    sender.sendMessage("[AuthMe] Please use that command in game");
                }
            } catch (NullPointerException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
            return true;
        } else if (args[0].equalsIgnoreCase("firstspawn")) {
            try {
                if (sender instanceof Player) {
                    if (Spawn.getInstance().getFirstSpawn() != null)
                        ((Player) sender).teleport(Spawn.getInstance().getFirstSpawn());
                    else sender.sendMessage("[AuthMe] First spawn has failed, please try to define the first spawn");
                } else {
                    sender.sendMessage("[AuthMe] Please use that command in game");
                }
            } catch (NullPointerException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
            return true;
        } else if (args[0].equalsIgnoreCase("changepassword") || args[0].equalsIgnoreCase("cp")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /authme changepassword <playername> <newpassword>");
                return true;
            }
            String lowpass = args[2].toLowerCase();
            if (lowpass.contains("delete") || lowpass.contains("where") || lowpass.contains("insert") || lowpass.contains("modify") || lowpass.contains("from") || lowpass.contains("select") || lowpass.contains(";") || lowpass.contains("null") || !lowpass.matches(Settings.getPassRegex)) {
                m.send(sender, "password_error");
                return true;
            }
            if (lowpass.equalsIgnoreCase(args[1])) {
                m.send(sender, "password_error_nick");
                return true;
            }
            if (lowpass.length() < Settings.getPasswordMinLen || lowpass.length() > Settings.passwordMaxLength) {
                m.send(sender, "pass_len");
                return true;
            }
            if (!Settings.unsafePasswords.isEmpty()) {
                if (Settings.unsafePasswords.contains(lowpass)) {
                    m.send(sender, "password_error_unsafe");
                    return true;
                }
            }
            final String name = args[1].toLowerCase();
            final String raw = args[2];
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    String hash;
                    try {
                        hash = PasswordSecurity.getHash(Settings.getPasswordHash, raw, name);
                    } catch (NoSuchAlgorithmException e) {
                        m.send(sender, "error");
                        return;
                    }
                    PlayerAuth auth = null;
                    if (PlayerCache.getInstance().isAuthenticated(name)) {
                        auth = PlayerCache.getInstance().getAuth(name);
                    } else if (plugin.database.isAuthAvailable(name)) {
                        auth = plugin.database.getAuth(name);
                    }
                    if (auth == null) {
                        m.send(sender, "unknown_user");
                        return;
                    }
                    auth.setHash(hash);
                    if (PasswordSecurity.userSalt.containsKey(name)) {
                        auth.setSalt(PasswordSecurity.userSalt.get(name));
                        plugin.database.updateSalt(auth);
                    }
                    if (!plugin.database.updatePassword(auth)) {
                        m.send(sender, "error");
                        return;
                    }
                    sender.sendMessage("pwd_changed");
                    ConsoleLogger.info(name + "'s password changed");
                }

            });
            return true;
        } else if (args[0].equalsIgnoreCase("unregister") || args[0].equalsIgnoreCase("unreg") || args[0].equalsIgnoreCase("del")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /authme unregister <playername>");
                return true;
            }
            String name = args[1].toLowerCase();
            if (!plugin.database.isAuthAvailable(name)) {
                m.send(sender, "user_unknown");
                return true;
            }
            if (!plugin.database.removeAuth(name)) {
                m.send(sender, "error");
                return true;
            }
            @SuppressWarnings("deprecation")
            Player target = Bukkit.getPlayer(name);
            PlayerCache.getInstance().removePlayer(name);
            Utils.setGroup(target, GroupType.UNREGISTERED);
            if (target != null) {
                if (target.isOnline()) {
                    if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
                        Location spawn = plugin.getSpawnLocation(target);
                        SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(target, target.getLocation(), spawn, false);
                        plugin.getServer().getPluginManager().callEvent(tpEvent);
                        if (!tpEvent.isCancelled()) {
                            target.teleport(tpEvent.getTo());
                        }
                    }
                    LimboCache.getInstance().addLimboPlayer(target);
                    int delay = Settings.getRegistrationTimeout * 20;
                    int interval = Settings.getWarnMessageInterval;
                    BukkitScheduler sched = sender.getServer().getScheduler();
                    if (delay != 0) {
                        BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, target), delay);
                        LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
                    }
                    LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("reg_msg"), interval)));
                    if (Settings.applyBlindEffect)
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
                    if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
                        target.setWalkSpeed(0.0f);
                        target.setFlySpeed(0.0f);
                    }
                    m.send(target, "unregistered");
                }
            }
            m.send(sender, "unregistered");
            ConsoleLogger.info(args[1] + " unregistered");
            return true;
        } else if (args[0].equalsIgnoreCase("purgelastpos") || args[0].equalsIgnoreCase("resetposition")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /authme purgelastpos <playername>");
                return true;
            }
            try {
                String name = args[1].toLowerCase();
                PlayerAuth auth = plugin.database.getAuth(name);
                if (auth == null) {
                    m.send(sender, "unknown_user");
                    return true;
                }
                auth.setQuitLocX(0D);
                auth.setQuitLocY(0D);
                auth.setQuitLocZ(0D);
                auth.setWorld("world");
                plugin.database.updateQuitLoc(auth);
                sender.sendMessage(name + "'s last position location is now reset");
            } catch (Exception e) {
                ConsoleLogger.showError("An error occured while trying to reset location or player do not exist, please see below: ");
                ConsoleLogger.showError(e.getMessage());
                if (sender instanceof Player)
                    sender.sendMessage("An error occured while trying to reset location or player do not exist, please see logs");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("switchantibot")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /authme switchantibot on/off");
                return true;
            }
            if (args[1].equalsIgnoreCase("on")) {
                plugin.switchAntiBotMod(true);
                sender.sendMessage("[AuthMe] AntiBotMod enabled");
                return true;
            }
            if (args[1].equalsIgnoreCase("off")) {
                plugin.switchAntiBotMod(false);
                sender.sendMessage("[AuthMe] AntiBotMod disabled");
                return true;
            }
            sender.sendMessage("Usage: /authme switchantibot on/off");
            return true;
        } else if (args[0].equalsIgnoreCase("getip")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /authme getip <onlineplayername>");
                return true;
            }
            @SuppressWarnings("deprecation")
            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("This player is not actually online");
                sender.sendMessage("Usage: /authme getip <onlineplayername>");
                return true;
            }
            sender.sendMessage(player.getName() + "'s actual IP is : " + player.getAddress().getAddress().getHostAddress() + ":" + player.getAddress().getPort());
            sender.sendMessage(player.getName() + "'s real IP is : " + plugin.getIP(player));
            return true;
        } else if (args[0].equalsIgnoreCase("forcelogin")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /authme forcelogin <playername>");
                return true;
            }
            try {
                @SuppressWarnings("deprecation")
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null || !player.isOnline()) {
                    sender.sendMessage("Player needs to be online!");
                    return true;
                }
                if (!plugin.authmePermissible(player, "authme.canbeforced")) {
                    sender.sendMessage("You cannot force login for this player!");
                    return true;
                }
                plugin.management.performLogin(player, "dontneed", true);
                sender.sendMessage("Force Login performed!");
            } catch (Exception e) {
                sender.sendMessage("An error occured while trying to get that player!");
            }
        } else if (args[0].equalsIgnoreCase("resetname")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    List<PlayerAuth> auths = plugin.database.getAllAuths();
                    for (PlayerAuth auth : auths) {
                        auth.setRealName("Player");
                        plugin.database.updateSession(auth);
                    }
                }
            });
        } else {
            sender.sendMessage("Usage: /authme reload|register playername password|changepassword playername password|unregister playername");
        }
        return true;
    }
}
