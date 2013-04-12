/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.whoami.authme.cache.backup;

/**
 *
 * @author stefano
 */
import java.io.File;
import java.io.FileWriter;

import java.util.Scanner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class FileCache {

	public FileCache() {
		final File folder = new File("cache");
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}

	public void createCache(String playername, DataFileCache playerData, String group, boolean operator) {
		final File file = new File("cache/" + playername
				+ ".cache");

		if (file.exists()) {
			return;
		}

		FileWriter writer = null;
		try {
			file.createNewFile();

			writer = new FileWriter(file);

			// line format Group|OperatorStatus
			if(operator)
				writer.write(group+";1\r\n");
			else writer.write(group+";0\r\n");
			writer.flush();

			ItemStack[] invstack = playerData.getInventory();

			for (int i = 0; i < invstack.length; i++) {

				int itemid = 0;
				int amount = 0;
				int durability = 0;
				String enchList = "";
				if (invstack[i] != null) {
					itemid = invstack[i].getTypeId();
					amount = invstack[i].getAmount();
					durability = invstack[i].getDurability();
					for(Enchantment e : invstack[i].getEnchantments().keySet()) {
						enchList = enchList.concat(e.getName()+":"+invstack[i].getEnchantmentLevel(e)+":");
					}
				}
				writer.write("i" + ":" + itemid + ":" + amount + ":"
						+ durability + ":"+ enchList + "\r\n");
				writer.flush();
			}

			ItemStack[] armorstack = playerData.getArmour();

			for (int i = 0; i < armorstack.length; i++) {
				int itemid = 0;
				int amount = 0;
				int durability = 0;
				String enchList = "";
				if (armorstack[i] != null) {
					itemid = armorstack[i].getTypeId();
					amount = armorstack[i].getAmount();
					durability = armorstack[i].getDurability();
					for(Enchantment e : armorstack[i].getEnchantments().keySet()) {
						enchList = enchList.concat(e.getName()+":"+armorstack[i].getEnchantmentLevel(e)+":");
					}                                        
				}
				writer.write("w" + ":" + itemid + ":" + amount + ":"
						+ durability + ":"+ enchList + "\r\n");
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
		if (!file.exists()) {
			return new DataFileCache(stacki, stacka);
		}

		Scanner reader = null;
		try {
			reader = new Scanner(file);

			int i = 0;
			int a = 0;
			while (reader.hasNextLine()) {
				final String line = reader.nextLine();

				if (!line.contains(":")) {
                                   // the fist line rapresent the player group and operator status
                                    final String[] playerInfo = line.split(";");
                                    group = playerInfo[0];
                                    
                                        if (Integer.parseInt(playerInfo[1]) == 1) {
                                            op = true;
                                        } else op = false;
                                        
                                    continue;
				}

				final String[] in = line.split(":");
				if (!in[0].equals("i") && !in[0].equals("w")) {
					continue;
				}
                        // can enchant item? size ofstring in file - 4  all / 2 = number of enchant
				if (in[0].equals("i")) {
					stacki[i] = new ItemStack(Integer.parseInt(in[1]),
					Integer.parseInt(in[2]), Short.parseShort((in[3])));
					if(in.length > 4 && !in[4].isEmpty()) {
						for(int k=4;k<in.length-1;k++) {
							stacki[i].addUnsafeEnchantment(Enchantment.getByName(in[k]) ,Integer.parseInt(in[k+1]));
							k++;
						}
					}
					i++;
				} else {
					stacka[a] = new ItemStack(Integer.parseInt(in[1]),
							Integer.parseInt(in[2]), Short.parseShort((in[3])));
					if(in.length > 4 && !in[4].isEmpty()) {
						for(int k=4;k<in.length-1;k++) {
							stacka[a].addUnsafeEnchantment(Enchantment.getByName(in[k]) ,Integer.parseInt(in[k+1]));
							k++;
						}
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
		return new DataFileCache(stacki, stacka, group, op);
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
