package fr.xephi.authme.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static fr.xephi.authme.listener.ListenerTestUtils.checkEventIsCanceledForUnauthed;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link EntityListener}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityListenerTest {

    @InjectMocks
    private EntityListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    public void shouldHandleSimpleEvents() {
        checkEventIsCanceledForUnauthed(listener, listenerService, EntityTargetEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, FoodLevelChangeEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, EntityShootBowEvent.class);
    }

    @Test
    public void shouldCancelEntityInteractEvent() {
        // given
        EntityInteractEvent event = mock(EntityInteractEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        // when
        listener.onLowestEntityInteract(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldNotCancelEntityInteractEvent() {
        // given
        EntityInteractEvent event = mock(EntityInteractEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        // when
        listener.onLowestEntityInteract(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verifyZeroInteractions(event);
    }

    @Test
    public void shouldCancelEntityInteractEventHighest() {
        // given
        EntityInteractEvent event = mock(EntityInteractEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        // when
        listener.onEntityInteract(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldNotCancelEntityInteractEventHighest() {
        // given
        EntityInteractEvent event = mock(EntityInteractEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        // when
        listener.onEntityInteract(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verifyZeroInteractions(event);
    }

    @Test
    public void shouldCancelRegainHealthEvent() {
        // given
        EntityRegainHealthEvent event = mock(EntityRegainHealthEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        // when
        listener.entityRegainHealthEvent(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verify(event).setCancelled(true);
        verify(event).setAmount(0);
    }

    @Test
    public void shouldNotCancelRegainedHealth() {
        // given
        EntityRegainHealthEvent event = mock(EntityRegainHealthEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        // when
        listener.entityRegainHealthEvent(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verifyZeroInteractions(event);
    }

    @Test
    public void shouldCancelEntityDamageByEntityEvent() {
        // given
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        Entity player = mock(Player.class);
        given(event.getDamager()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onAttack(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldNotCancelEntityDamageByEntityEvent() {
        // given
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        Entity player = mock(Player.class);
        given(event.getDamager()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onAttack(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(event, only()).getDamager();
    }

    @Test
    public void shouldCancelEntityDamageEvent() {
        // given
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        Entity entity = mock(Entity.class);
        given(event.getEntity()).willReturn(entity);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        // when
        listener.onDamage(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verify(event).setCancelled(true);
        verify(event).setDamage(0);
        verify(entity).setFireTicks(0);
    }

    @Test
    public void shouldNotCancelEntityDamageEvent() {
        // given
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        // when
        listener.onDamage(event);

        // then
        verify(listenerService).shouldCancelEvent(event);
        verifyZeroInteractions(event);
    }

}
