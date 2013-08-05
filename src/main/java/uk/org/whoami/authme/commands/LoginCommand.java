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

package uk.org.whoami.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.settings.Messages;

public class LoginCommand implements CommandExecutor {

    private AuthMe plugin;
    private Messages m = Messages.getInstance();

    public LoginCommand(AuthMe plugin) {
    	this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        final Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(m._("usage_log"));
            return true;
        }

        if (!plugin.authmePermissible(player, "authme." + label.toLowerCase())) {
            player.sendMessage(m._("no_perm"));
            return true;
        }
    	plugin.management.performLogin(player, args[0], false);
        return true;
    }
}
