package fr.xephi.authme;

import fr.xephi.authme.util.Wrapper;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class returning mocks for all calls in {@link Wrapper}.
 * This class keeps track of its mocks and will always return
 * the same one for each type.
 */
public class WrapperMock extends Wrapper {

    private static Map<Class<?>, Object> mocks = new HashMap<>();

    public WrapperMock() {
        this((AuthMe) getMock(AuthMe.class));
    }

    public WrapperMock(AuthMe authMe) {
        super(authMe);
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

    @SuppressWarnings("unchecked")
    private static <T> T getMock(Class<?> clazz) {
        Object o = mocks.get(clazz);
        if (o == null) {
            o = Mockito.mock(clazz);
            mocks.put(clazz, o);
        }
        return (T) o;
    }


}
