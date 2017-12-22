package fr.xephi.authme.data.limbo.persistence;

import ch.jalu.injector.factory.Factory;
import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.LimboSettings;
import org.bukkit.entity.Player;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.logging.Logger;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link LimboPersistence}.
 */
@RunWith(DelayedInjectionRunner.class)
public class LimboPersistenceTest {

    @InjectDelayed
    private LimboPersistence limboPersistence;

    @Mock
    private Factory<LimboPersistenceHandler> handlerFactory;

    @Mock
    private Settings settings;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    @SuppressWarnings("unchecked")
    public void setUpMocks() {
        given(settings.getProperty(LimboSettings.LIMBO_PERSISTENCE_TYPE)).willReturn(LimboPersistenceType.DISABLED);
        given(handlerFactory.newInstance(any(Class.class)))
            .willAnswer(invocation -> mock(invocation.getArgument(0)));
    }

    @Test
    public void shouldInitializeProperly() {
        // given / when / then
        assertThat(getHandler(), instanceOf(NoOpPersistenceHandler.class));
    }

    @Test
    public void shouldDelegateToHandler() {
        // given
        Player player = mock(Player.class);
        LimboPersistenceHandler handler = getHandler();
        LimboPlayer limbo = mock(LimboPlayer.class);
        given(handler.getLimboPlayer(player)).willReturn(limbo);

        // when
        LimboPlayer result = limboPersistence.getLimboPlayer(player);
        limboPersistence.saveLimboPlayer(player, mock(LimboPlayer.class));
        limboPersistence.removeLimboPlayer(mock(Player.class));

        // then
        assertThat(result, equalTo(limbo));
        verify(handler).getLimboPlayer(player);
        verify(handler).saveLimboPlayer(eq(player), argThat(notNullAndDifferentFrom(limbo)));
        verify(handler).removeLimboPlayer(argThat(notNullAndDifferentFrom(player)));
    }

    @Test
    public void shouldReloadProperly() {
        // given
        given(settings.getProperty(LimboSettings.LIMBO_PERSISTENCE_TYPE))
            .willReturn(LimboPersistenceType.INDIVIDUAL_FILES);

        // when
        limboPersistence.reload(settings);

        // then
        assertThat(getHandler(), instanceOf(LimboPersistenceType.INDIVIDUAL_FILES.getImplementationClass()));
    }

    @Test
    public void shouldHandleExceptionWhenGettingLimbo() {
        // given
        Player player = mock(Player.class);
        Logger logger = TestHelper.setupLogger();
        LimboPersistenceHandler handler = getHandler();
        doThrow(RuntimeException.class).when(handler).getLimboPlayer(player);

        // when
        LimboPlayer result = limboPersistence.getLimboPlayer(player);

        // then
        assertThat(result, nullValue());
        verify(logger).warning(argThat(containsString("[RuntimeException]")));
    }

    @Test
    public void shouldHandleExceptionWhenSavingLimbo() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limbo = mock(LimboPlayer.class);
        Logger logger = TestHelper.setupLogger();
        LimboPersistenceHandler handler = getHandler();
        doThrow(IllegalStateException.class).when(handler).saveLimboPlayer(player, limbo);

        // when
        limboPersistence.saveLimboPlayer(player, limbo);

        // then
        verify(logger).warning(argThat(containsString("[IllegalStateException]")));
    }

    @Test
    public void shouldHandleExceptionWhenRemovingLimbo() {
        // given
        Player player = mock(Player.class);
        Logger logger = TestHelper.setupLogger();
        LimboPersistenceHandler handler = getHandler();
        doThrow(UnsupportedOperationException.class).when(handler).removeLimboPlayer(player);

        // when
        limboPersistence.removeLimboPlayer(player);

        // then
        verify(logger).warning(argThat(containsString("[UnsupportedOperationException]")));
    }

    private LimboPersistenceHandler getHandler() {
        return ReflectionTestUtils.getFieldValue(LimboPersistence.class, limboPersistence, "handler");
    }

    private static <T> Matcher<T> notNullAndDifferentFrom(T o) {
        return both(not(sameInstance(o))).and(not(nullValue()));
    }
}
