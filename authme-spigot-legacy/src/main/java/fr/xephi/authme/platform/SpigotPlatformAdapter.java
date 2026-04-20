package fr.xephi.authme.platform;

/**
 * Platform adapter for Spigot 1.16–1.19 (legacy versions).
 */
public class SpigotPlatformAdapter extends AbstractSpigotPlatformAdapter {

    @Override
    public String getPlatformName() {
        return "spigot-legacy";
    }
}
