package fr.xephi.authme.data.limbo.persistence;

import ch.jalu.injector.factory.Factory;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.LimboSettings;
import org.bukkit.entity.Player;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
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
@ExtendWith(MockitoExtension.class)
class LimboPersistenceTest {

    private LimboPersistence limboPersistence;

    @Mock
    private Factory<LimboPersistenceHandler> handlerFactory;

    @Mock
    private Settings settings;

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpMocksAndLimboPersistence() {
        given(settings.getProperty(LimboSettings.LIMBO_PERSISTENCE_TYPE)).willReturn(LimboPersistenceType.DISABLED);
        given(handlerFactory.newInstance(any(Class.class)))
            .willAnswer(invocation -> mock((Class<?>) invocation.getArgument(0)));
        limboPersistence = new LimboPersistence(settings, handlerFactory);
    }

    @Test
    void shouldInitializeProperly() {
        // given / when / then
        assertThat(getHandler(), instanceOf(NoOpPersistenceHandler.class));
    }

    @Test
    void shouldDelegateToHandler() {
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
    void shouldReloadProperly() {
        // given
        given(settings.getProperty(LimboSettings.LIMBO_PERSISTENCE_TYPE))
            .willReturn(LimboPersistenceType.INDIVIDUAL_FILES);

        // when
        limboPersistence.reload(settings);

        // then
        assertThat(getHandler(), instanceOf(LimboPersistenceType.INDIVIDUAL_FILES.getImplementationClass()));
    }

    @Test
    void shouldHandleExceptionWhenGettingLimbo() {
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
    void shouldHandleExceptionWhenSavingLimbo() {
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
    void shouldHandleExceptionWhenRemovingLimbo() {
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
