package fr.xephi.authme.cache.backup;

import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import fr.xephi.authme.api.API;

public class FileCache {

	public FileCache() {
		final File folder = new File("cache");
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}

	public void createCache(String playername, DataFileCache playerData, String group, boolean operator, boolean flying) {
		final File file = new File("cache/" + playername
				+ ".cache");

		if (file.exists()) {
			return;
		}

		FileWriter writer = null;
		try {
			file.createNewFile();

			writer = new FileWriter(file);

			String s = group+";";
			if (operator)
				s = s + "1";
			else s = s + "0";

			// line format Group|OperatorStatus|isFlying
			if(flying)
				writer.write(s+";1" + API.newline);
			else writer.write(s+";0" + API.newline);
			writer.flush();

			ItemStack[] invstack = playerData.getInventory();

			for (int i = 0; i < invstack.length; i++) {

				String itemid = "AIR";
				int amount = 0;
				int durability = 0;
				String enchList = "";
				String name = "";
				String lores = "";
				if (invstack[i] != null) {
					itemid = invstack[i].getType().name();
					amount = invstack[i].getAmount();
					durability = invstack[i].getDurability();
					for(Enchantment e : invstack[i].getEnchantments().keySet()) {
						enchList = enchList.concat(e.getName()+":"+invstack[i].getEnchantmentLevel(e)+":");
					}
					if (enchList.length() > 1)
						enchList = enchList.substring(0, enchList.length() - 1);
					if (invstack[i].hasItemMeta()) {
						if (invstack[i].getItemMeta().hasDisplayName()) {
							name = invstack[i].getItemMeta().getDisplayName();
						}
						if (invstack[i].getItemMeta().hasLore()) {
							for (String lore : invstack[i].getItemMeta().getLore()) {
								lores = lore + "%newline%";
							}
						}
					}
				}
				String writeItem = "i" + ":" + itemid + ":" + amount + ":"
				+ durability + ":"+ enchList + ";" + name + "\\*" + lores + "\r\n";
				writer.write(writeItem);
				writer.flush();
			}

			ItemStack[] armorstack = playerData.getArmour();

			for (int i = 0; i < armorstack.length; i++) {
				String itemid = "AIR";
				int amount = 0;
				int durability = 0;
				String enchList = "";
				String name = "";
				String lores = "";
				if (armorstack[i] != null) {
					itemid = armorstack[i].getType().name();
					amount = armorstack[i].getAmount();
					durability = armorstack[i].getDurability();
					for(Enchantment e : armorstack[i].getEnchantments().keySet()) {
						enchList = enchList.concat(e.getName()+":"+armorstack[i].getEnchantmentLevel(e)+":");
					}
					if (enchList.length() > 1)
						enchList = enchList.substring(0, enchList.length() - 1);
					if (armorstack[i].hasItemMeta()) {
						if (armorstack[i].getItemMeta().hasDisplayName()) {
							name = armorstack[i].getItemMeta().getDisplayName();
						}
						if (armorstack[i].getItemMeta().hasLore()) {
							for (String lore : armorstack[i].getItemMeta().getLore()) {
								lores = lore + "%newline%";
							}
						}
					}
				}
				String writeItem = "w" + ":" + itemid + ":" + amount + ":"
				+ durability + ":"+ enchList + ";" + name + "\\*" + lores + "\r\n";
				writer.write(writeItem);
				writer.flush();
			}
			writer.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public DataFileCache readCache(String playername) {
		final File file = new File("cache/" + playername
				+ ".cache");

		ItemStack[] stacki = new ItemStack[36];
		ItemStack[] stacka = new ItemStack[4];
		String group = null;
		boolean op = false;
		boolean flying = false;
		if (!file.exists()) {
			return new DataFileCache(stacki, stacka);
		}

		Scanner reader = null;
		try {
			reader = new Scanner(file);

			int i = 0;
			int a = 0;
			while (reader.hasNextLine()) {
				String line = reader.nextLine();

				if (!line.contains(":")) {
                                   // the fist line represent the player group, operator status and flying status
                                    final String[] playerInfo = line.split(";");
                                    group = playerInfo[0];

                                        if (Integer.parseInt(playerInfo[1]) == 1) {
                                            op = true;
                                        } else op = false;
                                        if (playerInfo.length > 2) {
                                        	if (Integer.parseInt(playerInfo[2]) == 1)
                                        		flying = true;
                                        	else flying = false;
                                        }

                                    continue;
				}

				if (!line.startsWith("i") && !line.startsWith("w")) {
					continue;
				}
				String lores = "";
				String name = "";
				if (line.split("\\*").length > 1) {
					lores = line.split("\\*")[1];
					line = line.split("\\*")[0];
				}
				if (line.split(";").length > 1) {
					name = line.split(";")[1];
					line = line.split(";")[0];
				}
				final String[] in = line.split(":");
                // can enchant item? size ofstring in file - 4  all / 2 = number of enchant
				if (in[0].equals("i")) {
					stacki[i] = new ItemStack(Material.getMaterial(in[1]),
					Integer.parseInt(in[2]), Short.parseShort((in[3])));
					if(in.length > 4 && !in[4].isEmpty()) {
						for(int k=4;k<in.length-1;k++) {
							stacki[i].addUnsafeEnchantment(Enchantment.getByName(in[k]) ,Integer.parseInt(in[k+1]));
							k++;
						}
					}
					if (!name.isEmpty())
						stacki[i].getItemMeta().setDisplayName(name);
					if (!lores.isEmpty()) {
						List<String> loreList = new ArrayList<String>();
						for (String s : lores.split("%newline%")) {
							loreList.add(s);
						}
						stacki[i].getItemMeta().setLore(loreList);
					}
					i++;
				} else {
					stacka[a] = new ItemStack(Material.getMaterial(in[1]),
							Integer.parseInt(in[2]), Short.parseShort((in[3])));
					if(in.length > 4 && !in[4].isEmpty()) {
						for(int k=4;k<in.length-1;k++) {
							stacka[a].addUnsafeEnchantment(Enchantment.getByName(in[k]) ,Integer.parseInt(in[k+1]));
							k++;
						}
					}
					if (!name.isEmpty())
						stacka[a].getItemMeta().setDisplayName(name);
					if (!lores.isEmpty()) {
						List<String> loreList = new ArrayList<String>();
						for (String s : lores.split("%newline%")) {
							loreList.add(s);
						}
						stacka[a].getItemMeta().setLore(loreList);
					}
					a++;
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return new DataFileCache(stacki, stacka, group, op, flying);
	}

	public void removeCache(String playername) {
		final File file = new File("cache/" + playername
				+ ".cache");

		if (file.exists()) {
			file.delete();
		}
	}

	public boolean doesCacheExist(String playername) {
		final File file = new File("cache/" + playername
				+ ".cache");

		if (file.exists()) {
			return true;
		}
		return false;
	}

}
