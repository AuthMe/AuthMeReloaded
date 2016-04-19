package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.output.Messages;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Class returning mocks for all calls in {@link Wrapper}.
 * This class keeps track of its mocks and will always return
 * the same one for each type.
 */
public class WrapperMock extends Wrapper {

    private Map<Class<?>, Object> mocks = new HashMap<>();

    private WrapperMock() {
        super();
    }

    /**
     * Create a new instance of the WrapperMock and inject it as singleton into the Wrapper class.
     *
     * @return The created singleton
     */
    public static WrapperMock createInstance() {
        WrapperMock instance = new WrapperMock();
        Wrapper.setSingleton(instance);
        return instance;
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
        return new File("/");
    }

    /**
     * Return whether a mock of the given class type was created, i.e. verify whether a certain method was executed on
     * the Wrapper to retrieve an entity.
     *
     * @param mockClass The class of the mock to verify
     *
     * @return True if the mock has been created, false otherwise
     */
    public boolean wasMockCalled(Class<?> mockClass) {
        return mocks.get(mockClass) != null;
    }

    private <T> T getMock(Class<T> clazz) {
        Object o = mocks.get(clazz);
        if (o == null) {
            o = Mockito.mock(clazz);
            mocks.put(clazz, o);
        }
        return clazz.cast(o);
    }


}
