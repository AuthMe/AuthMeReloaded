package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Messages;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class returning mocks for all calls in {@link Wrapper}.
 * This class keeps track of its mocks and will always return
 * the same one for each type.
 */
public class WrapperMock extends Wrapper {

    private Map<Class<?>, Object> mocks = new HashMap<>();
    private static WrapperMock singleton;
    private File getDataFolderValue;

    private WrapperMock() {
        super();
    }

    /**
     * Create a new instance of the WrapperMock and inject it as singleton into Wrapper.
     *
     * @return The created singleton
     */
    public static WrapperMock createInstance() {
        singleton = new WrapperMock();
        Wrapper.setSingleton(singleton);
        return singleton;
    }

    /**
     * Return the WrapperMock singleton or null if it hasn't been initialized. To avoid confusion, it may be best to
     * only call {@link WrapperMock#createInstance()} and to keep a reference to the returned singleton.
     *
     * @return The singleton or null
     */
    public static WrapperMock getInstance() {
        return singleton;
    }

    @Override
    public Logger getLogger() {
        return getMock(Logger.class);
    }

    @Override
    public Server getServer() {
        return getMock(Server.class);
    }

    @Override
    public AuthMe getAuthMe() {
        return getMock(AuthMe.class);
    }

    @Override
    public BukkitScheduler getScheduler() {
        return getMock(BukkitScheduler.class);
    }

    @Override
    public Messages getMessages() {
        return getMock(Messages.class);
    }

    @Override
    public PlayerCache getPlayerCache() {
        return getMock(PlayerCache.class);
    }

    @Override
    public File getDataFolder() {
        if (singleton.getDataFolderValue != null) {
            return singleton.getDataFolderValue;
        }
        return getMock(File.class);
    }

    public void setDataFolder(File file) {
        this.getDataFolderValue = file;
    }

    @SuppressWarnings("unchecked")
    private <T> T getMock(Class<?> clazz) {
        Object o = mocks.get(clazz);
        if (o == null) {
            o = Mockito.mock(clazz);
            mocks.put(clazz, o);
        }
        return (T) o;
    }


}
