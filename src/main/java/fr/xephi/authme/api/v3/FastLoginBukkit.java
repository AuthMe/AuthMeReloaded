/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2021 <Your name and contributors>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.command.CrackedCommand;
import com.github.games647.fastlogin.bukkit.command.PremiumCommand;
import com.github.games647.fastlogin.bukkit.listener.ConnectionListener;
import com.github.games647.fastlogin.bukkit.listener.PaperCacheListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.ManualNameChange;
import com.github.games647.fastlogin.bukkit.listener.protocollib.ProtocolLibListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.SkinApplyListener;
import com.github.games647.fastlogin.bukkit.listener.protocolsupport.ProtocolSupportListener;
import com.github.games647.fastlogin.bukkit.task.DelayedAuthHook;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.PremiumStatus;
import com.github.games647.fastlogin.core.hooks.bedrock.BedrockService;
import com.github.games647.fastlogin.core.hooks.bedrock.FloodgateService;
import com.github.games647.fastlogin.core.hooks.bedrock.GeyserService;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;

import io.papermc.lib.PaperLib;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.GeyserImpl;
import org.slf4j.Logger;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLoginBukkit extends JavaPlugin implements PlatformPlugin<CommandSender> {

    //1 minutes should be enough as a timeout for bad internet connection (Server, Client and Mojang)
    private final ConcurrentMap<String, BukkitLoginSession> loginSession = CommonUtil.buildCache(1, -1);
    private final Map<UUID, PremiumStatus> premiumPlayers = new ConcurrentHashMap<>();
    private final Logger logger;

    private boolean serverStarted;
    private BungeeManager bungeeManager;
    private final BukkitScheduler scheduler;
    private FastLoginCore<Player, CommandSender, FastLoginBukkit> core;
    private FloodgateService floodgateService;
    private GeyserService geyserService;

    private PremiumPlaceholder premiumPlaceholder;

    public FastLoginBukkit() {
        this.logger = CommonUtil.initializeLoggerService(getLogger());
        this.scheduler = new BukkitScheduler(this, logger, getThreadFactory());
    }

    @Override
    public void onEnable() {
        core = new FastLoginCore<>(this);
        core.load();

        if (getServer().getOnlineMode()) {
            //we need to require offline to prevent a loginSession request for an offline player
            logger.error("Server has to be in offline mode");
            setEnabled(false);
            return;
        }

        if (!initializeFloodgate()) {
            setEnabled(false);
        }

        bungeeManager = new BungeeManager(this);
        bungeeManager.initialize();

        PluginManager pluginManager = getServer().getPluginManager();
        if (bungeeManager.isEnabled()) {
            markInitialized();
        } else {
            if (!core.setupDatabase()) {
                setEnabled(false);
                return;
            }

            if (pluginManager.isPluginEnabled("ProtocolSupport")) {
                pluginManager.registerEvents(new ProtocolSupportListener(this, core.getRateLimiter()), this);
            } else if (pluginManager.isPluginEnabled("ProtocolLib")) {
                ProtocolLibListener.register(this, core.getRateLimiter());

                if (isPluginInstalled("floodgate")) {
                    if (getConfig().getBoolean("floodgatePrefixWorkaround")){
                        ManualNameChange.register(this, floodgateService);
                        logger.info("Floodgate prefix injection workaround has been enabled.");
                        logger.info("If you have problems joining the server, try disabling it in the configuration.");
                    } else {
                        logger.warn("We have detected that you are runnging FastLogin alongside Floodgate and ProtocolLib.");
                        logger.warn("Currently there is an issue with FastLogin that prevents Floodgate name prefixes from showing up "
                                + "when it is together used with ProtocolLib.");
                        logger.warn("If you would like to use Floodgate name prefixes, you can enable an experimental workaround by changing "
                                + "the value 'floodgatePrefixWorkaround' to true in config.yml.");
                        logger.warn("For more information visit https://github.com/games647/FastLogin/issues/493");
                    }
                }

                //if server is using paper - we need to set the skin at pre login anyway, so no need for this listener
                if (!PaperLib.isPaper() && getConfig().getBoolean("forwardSkin")) {
                    pluginManager.registerEvents(new SkinApplyListener(this), this);
                }
            } else {
                logger.warn("Either ProtocolLib or ProtocolSupport have to be installed if you don't use BungeeCord");
                setEnabled(false);
                return;
            }
        }

        //delay dependency setup because we load the plugin very early where plugins are initialized yet
        getServer().getScheduler().runTaskLater(this, new DelayedAuthHook(this), 5L);

        pluginManager.registerEvents(new ConnectionListener(this), this);

        //if server is using paper - we need to add one more listener to correct the user cache usage
        if (PaperLib.isPaper()) {
            pluginManager.registerEvents(new PaperCacheListener(this), this);
        }

        //register commands using a unique name
        Optional.ofNullable(getCommand("premium")).ifPresent(c -> c.setExecutor(new PremiumCommand(this)));
        Optional.ofNullable(getCommand("cracked")).ifPresent(c -> c.setExecutor(new CrackedCommand(this)));

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            premiumPlaceholder = new PremiumPlaceholder(this);
            premiumPlaceholder.register();
        }

        dependencyWarnings();
    }

    private boolean initializeFloodgate() {
        if (getServer().getPluginManager().getPlugin("Geyser-Spigot") != null) {
            geyserService = new GeyserService(GeyserImpl.getInstance(), core);
        }

        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateService = new FloodgateService(FloodgateApi.getInstance(), core);

            // Check Floodgate config values and return
            return floodgateService.isValidFloodgateConfigString("autoLoginFloodgate")
                    && floodgateService.isValidFloodgateConfigString("allowFloodgateNameConflict");
        }

        return true;
    }

    @Override
    public void onDisable() {
        loginSession.clear();
        premiumPlayers.clear();

        if (core != null) {
            core.close();
        }

        if (bungeeManager != null) {
            bungeeManager.cleanup();
        }

        if (premiumPlaceholder != null && getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                premiumPlaceholder.unregister();
            } catch (Exception | NoSuchMethodError exception) {
                logger.error("Failed to unregister placeholder", exception);
            }
        }
    }

    public FastLoginCore<Player, CommandSender, FastLoginBukkit> getCore() {
        return core;
    }

    /**
     * Gets a thread-safe map about players which are connecting to the server are being checked to be premium (paid
     * account)
     *
     * @return a thread-safe loginSession map
     */
    public ConcurrentMap<String, BukkitLoginSession> getLoginSessions() {
        return loginSession;
    }

    public BukkitLoginSession getSession(InetSocketAddress address) {
        String id = getSessionId(address);
        return loginSession.get(id);
    }

    public String getSessionId(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ':' + address.getPort();
    }

    public void putSession(InetSocketAddress address, BukkitLoginSession session) {
        String id = getSessionId(address);
        loginSession.put(id, session);
    }

    public void removeSession(InetSocketAddress address) {
        String id = getSessionId(address);
        loginSession.remove(id);
    }

    public Map<UUID, PremiumStatus> getPremiumPlayers() {
        return premiumPlayers;
    }

    /**
     * Fetches the premium status of an online player.
     *
     * @param onlinePlayer player that is currently online player (play state)
     * @return the online status or unknown if an error happened, the player isn't online or BungeeCord doesn't send
     * us the status message yet (This means you cannot check the login status on the PlayerJoinEvent).
     */
    public PremiumStatus getStatus(UUID onlinePlayer) {
        return premiumPlayers.getOrDefault(onlinePlayer, PremiumStatus.UNKNOWN);
    }

    /**
     * Wait before the server is fully started. This is workaround, because connections right on startup are not
     * injected by ProtocolLib
     *
     * @return true if ProtocolLib can now intercept packets
     */
    public boolean isServerFullyStarted() {
        return serverStarted;
    }

    public void markInitialized() {
        this.serverStarted = true;
    }

    public BungeeManager getBungeeManager() {
        return bungeeManager;
    }

    @Override
    public Path getPluginFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public Logger getLog() {
        return logger;
    }

    @Override
    public BukkitScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void sendMessage(CommandSender receiver, String message) {
        receiver.sendMessage(message);
    }

	/**
	 * Checks if a plugin is installed on the server
	 * @param name the name of the plugin
	 * @return true if the plugin is installed
	 */
	@Override
	public boolean isPluginInstalled(String name) {
	    // the plugin may be enabled after FastLogin, so isPluginEnabled() won't work here
	    return Bukkit.getServer().getPluginManager().getPlugin(name) != null;
	}

    public FloodgateService getFloodgateService() {
        return floodgateService;
    }

    public GeyserService getGeyserService() {
        return geyserService;
    }

    @Override
    public BedrockService<?> getBedrockService() {
        if (floodgateService != null) {
            return floodgateService;
        }
        return geyserService;
    }

    /**
     * Send warning messages to log if incompatible plugins are used
     */
    private void dependencyWarnings() {
        if (isPluginInstalled("floodgate-bukkit")) {
            logger.warn("We have detected that you are running Floodgate 1.0 which is not supported by the Bukkit "
                    + "version of FastLogin.");
            logger.warn("If you would like to use FastLogin with Floodgate, you can download development builds of "
                    + "Floodgate 2.0 from https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/dev%252F2.0/");
            logger.warn("Don't forget to update Geyser to a supported version as well from "
                    + "https://ci.opencollab.dev/job/GeyserMC/job/Geyser/job/floodgate-2.0/");
    	}
    }
}
