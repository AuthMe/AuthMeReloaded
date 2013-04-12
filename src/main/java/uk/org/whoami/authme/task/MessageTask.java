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

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;

public class MessageTask implements Runnable {

    private AuthMe plugin;
    private String name;
    private String msg;
    private int interval;

    public MessageTask(AuthMe plugin, String name, String msg, int interval) {
        this.plugin = plugin;
        this.name = name;
        this.msg = msg;
        this.interval = interval;
    }

    @Override
    public void run() {
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().toLowerCase().equals(name)) {
                player.sendMessage(msg);
                BukkitScheduler sched = plugin.getServer().getScheduler();
                BukkitTask late = sched.runTaskLater(plugin, this, interval * 20);
                if(LimboCache.getInstance().hasLimboPlayer(name)) {
                	LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(late.getTaskId());
                }
            }
        }
    }
}
