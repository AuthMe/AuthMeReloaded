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

package uk.org.whoami.authme.cache.limbo;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class LimboPlayer {

    private String name;
    private ItemStack[] inventory;
    private ItemStack[] armour;
    private Location loc = null;
    private int timeoutTaskId = -1;
    private int messageTaskId = -1;
    private int gameMode = 0;
    private boolean operator = false;
    private String group = null;

    public LimboPlayer(String name, Location loc, ItemStack[] inventory, ItemStack[] armour, int gameMode, boolean operator, String group) {
        this.name = name;
        this.loc = loc;
        this.inventory = inventory;
        this.armour = armour;
        this.gameMode = gameMode;
        this.operator = operator;
        this.group = group;
    }

    public LimboPlayer(String name, Location loc, int gameMode, boolean operator, String group) {
        this.name = name;
        this.loc = loc;
        this.gameMode = gameMode;
        this.operator = operator;
        this.group = group;
    }

    public LimboPlayer(String name, String group) {
        this.name = name;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public Location getLoc() {
        return loc;
    }

    public ItemStack[] getArmour() {
        return armour;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public int getGameMode() {
        return gameMode;
    }

    public boolean getOperator() {
        return operator;
    }

    public String getGroup() {
        return group;
    }

	public void setTimeoutTaskId(int i) {
		this.timeoutTaskId = i;
	}

	public int getTimeoutTaskId() {
		return timeoutTaskId;
	}

	public void setMessageTaskId(int messageTaskId) {
		this.messageTaskId = messageTaskId;
	}

	public int getMessageTaskId() {
		return messageTaskId;
	}

}
