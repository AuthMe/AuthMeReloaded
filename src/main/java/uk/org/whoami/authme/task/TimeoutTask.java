/*
 * Copyright 2011 Sebastian Köhler <sebkoehler@whoami.org.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.whoami.authme.task;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.limbo.LimboPlayer;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.listener.AuthMePlayerListener;
import uk.org.whoami.authme.settings.Messages;


public class TimeoutTask implements Runnable {

    private JavaPlugin plugin;
    private String name;
    private Messages m = Messages.getInstance();
    private FileCache playerCache = new FileCache();

    public TimeoutTask(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void run() {
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().toLowerCase().equals(name)) {
                if (LimboCache.getInstance().hasLimboPlayer(name)) {
                    LimboPlayer inv = LimboCache.getInstance().getLimboPlayer(name);
                    player.getServer().getScheduler().cancelTask(inv.getTimeoutTaskId());
                    if(playerCache.doesCacheExist(name)) {
                        playerCache.removeCache(name);
                    } 
                } 
                int gm = AuthMePlayerListener.gameMode.get(name);
            	player.setGameMode(GameMode.getByValue(gm));
            	ConsoleLogger.info("Set " + player.getName() + " to gamemode: " + GameMode.getByValue(gm).name());
                player.kickPlayer(m._("timeout"));
                break;
            }
        }
    }
}
