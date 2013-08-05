package uk.org.whoami.authme;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import uk.org.whoami.authme.events.AuthMeTeleportEvent;
import uk.org.whoami.authme.settings.Settings;

/**
 *
 * @author stefano
 */
public class Utils {
     private String currentGroup;
     private static Utils singleton;
     private String unLoggedGroup = Settings.getUnloggedinGroup;
     BukkitTask id;

  public void setGroup(Player player, groupType group) {
    if (!player.isOnline())
        return;
    if(!Settings.isPermissionCheckEnabled)
        return;

        switch(group) {
            case UNREGISTERED: {
                currentGroup = AuthMe.permission.getPrimaryGroup(player);
                AuthMe.permission.playerRemoveGroup(player, currentGroup);
                AuthMe.permission.playerAddGroup(player, Settings.unRegisteredGroup);
                break;
            }
            case REGISTERED: {
                currentGroup = AuthMe.permission.getPrimaryGroup(player);
                AuthMe.permission.playerRemoveGroup(player, currentGroup);
                AuthMe.permission.playerAddGroup(player, Settings.getRegisteredGroup);
                break;
            }                
        }
        return;
    }

    public String removeAll(Player player) {

        if(!Utils.getInstance().useGroupSystem()){
            return null;
        }
        if( !Settings.getJoinPermissions.isEmpty() ) {
            hasPermOnJoin(player);
        }
        this.currentGroup = AuthMe.permission.getPrimaryGroup(player.getWorld(),player.getName().toString());
        if(AuthMe.permission.playerRemoveGroup(player.getWorld(),player.getName().toString(), currentGroup) && AuthMe.permission.playerAddGroup(player.getWorld(),player.getName().toString(),this.unLoggedGroup)) {
            return currentGroup;
        }
        return null;
    }

    public boolean addNormal(Player player, String group) {
       if(!Utils.getInstance().useGroupSystem()){
            return false;
        }   
        if(AuthMe.permission.playerRemoveGroup(player.getWorld(),player.getName().toString(),this.unLoggedGroup) && AuthMe.permission.playerAddGroup(player.getWorld(),player.getName().toString(),group)) {
            return true;
        }
        return false;
    }

    private String hasPermOnJoin(Player player) {

    	Iterator<String> iter = Settings.getJoinPermissions.iterator();
    	while (iter.hasNext()) {
    		String permission = iter.next();
    		if(AuthMe.permission.playerHas(player, permission)){
    			AuthMe.permission.playerAddTransient(player, permission);
    		}
    	}
    	return null;
    }

    public boolean isUnrestricted(Player player) {

        if(Settings.getUnrestrictedName.isEmpty() || Settings.getUnrestrictedName == null)
            return false;

        if(Settings.getUnrestrictedName.contains(player.getName()))
            return true;

        return false;

    }
     public static Utils getInstance() {

    	 singleton = new Utils();

    	 return singleton;
    } 

    private boolean useGroupSystem() {

        if(Settings.isPermissionCheckEnabled && !Settings.getUnloggedinGroup.isEmpty()) {
            return true;
        } return false;
    }

    public void packCoords(int x, int y, int z, String w, final Player pl)
    {
    	World theWorld;
    	if (w.equals("unavailableworld")) {
    		theWorld = pl.getWorld();
    	} else {
    		theWorld = Bukkit.getWorld(w);
    	}
    	if (theWorld == null)
			theWorld = pl.getWorld();
    	final World world = theWorld;
    	final int fY = y;
    	final Location locat = new Location(world, x, y + 0.6D, z);
    	final Location loc = locat.getBlock().getLocation();

    	Bukkit.getScheduler().scheduleSyncDelayedTask(AuthMe.getInstance(), new Runnable() {
			@Override
			public void run() {
		        AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(pl, loc);
		        AuthMe.getInstance().getServer().getPluginManager().callEvent(tpEvent);
		        if(!tpEvent.isCancelled()) {
		        	if (!tpEvent.getTo().getChunk().isLoaded())
		        		tpEvent.getTo().getChunk().load();
		      	  pl.teleport(tpEvent.getTo());
		        }
			}
    	});

    	id = Bukkit.getScheduler().runTaskTimer(AuthMe.authme, new Runnable()
    	{
    		@Override
    		public void run() {
    			int current = (int)pl.getLocation().getY();
    			World currentWorld = pl.getWorld();
    			if (current != fY && world.getName() == currentWorld.getName()) {
    				pl.teleport(loc);
    			}
    		}
    	}, 1L, 20L);
    	Bukkit.getScheduler().scheduleSyncDelayedTask(AuthMe.authme, new Runnable()
    	{
		@Override
		public void run() {
			id.cancel();
		}
      }, 60L);
      }

	/*
     * Random Token for passpartu
     * 
     */
    public boolean obtainToken() {
        File file = new File("plugins/AuthMe/passpartu.token");

	if (file.exists())
            file.delete();

		FileWriter writer = null;
		try {
			file.createNewFile();
			writer = new FileWriter(file);
                        String token = generateToken();
                        writer.write(token+":"+System.currentTimeMillis()/1000+"\r\n");
                        writer.flush();
                        ConsoleLogger.info("[AuthMe] Security passpartu token: "+ token);
                        writer.close();
                        return true;
                } catch(Exception e) {
                    e.printStackTrace(); 
                }
        return false;
    }

    /*
     * Read Toekn
     */
    public boolean readToken(String inputToken) {
        File file = new File("plugins/AuthMe/passpartu.token");

	if (!file.exists()) 	
            return false;

        if (inputToken.isEmpty() )
            return false;
		Scanner reader = null;
		try {
			reader = new Scanner(file);
			while (reader.hasNextLine()) {
				final String line = reader.nextLine();
				if (line.contains(":")) {   
                                   String[] tokenInfo = line.split(":");
                                   if(tokenInfo[0].equals(inputToken) && System.currentTimeMillis()/1000-30 <= Integer.parseInt(tokenInfo[1]) ) { 
                                       file.delete();
                                       reader.close();
                                       return true;
                                   }
                                } 
                        }
                } catch(Exception e) {
                    e.printStackTrace(); 
                }
	reader.close();        
        return false;
    }
    /*
     * Generate Random Token
     */
    private String generateToken() {
           // obtain new random token
           Random rnd = new Random ();
            char[] arr = new char[5];
            for (int i=0; i<5; i++) {
                    int n = rnd.nextInt (36);
                    arr[i] = (char) (n < 10 ? '0'+n : 'a'+n-10);
            }
            return new String(arr);        
    }

    public enum groupType {
        UNREGISTERED, REGISTERED, NOTLOGGEDIN, LOGGEDIN
    }

}
