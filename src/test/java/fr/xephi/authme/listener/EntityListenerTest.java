package fr.xephi.authme.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.listener.EventCancelVerifier.withServiceMock;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
        withServiceMock(listenerService)
            .check(listener::onFoodLevelChange, FoodLevelChangeEvent.class)
            .check(listener::onShoot, EntityShootBowEvent.class)
            .check(listener::onEntityInteract, EntityInteractEvent.class)
            .check(listener::onLowestEntityInteract, EntityInteractEvent.class);
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
        verifyNoInteractions(event);
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
        verifyNoInteractions(event);
    }

    @Test
    public void shouldAllowProjectileLaunchFromNonHuman() {
        // given
        Projectile projectile = mock(Projectile.class);
        ProjectileSource source = mock(ProjectileSource.class);
        given(projectile.getShooter()).willReturn(source);
        ProjectileLaunchEvent event = mock(ProjectileLaunchEvent.class);
        given(event.getEntity()).willReturn(projectile);

        // when
        listener.onProjectileLaunch(event);

        // then
        verifyNoInteractions(listenerService);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    public void shouldAllowProjectileLaunchFromAuthedHuman() {
        // given
        Projectile projectile = mock(Projectile.class);
        Player player = mock(Player.class);
        given(projectile.getShooter()).willReturn(player);
        ProjectileLaunchEvent event = mock(ProjectileLaunchEvent.class);
        given(event.getEntity()).willReturn(projectile);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onProjectileLaunch(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    public void shouldRejectProjectileLaunchFromUnauthed() {
        // given
        Projectile projectile = mock(Projectile.class);
        Player player = mock(Player.class);
        given(projectile.getShooter()).willReturn(player);
        ProjectileLaunchEvent event = mock(ProjectileLaunchEvent.class);
        given(event.getEntity()).willReturn(projectile);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onProjectileLaunch(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldHandleOldShooterMethod() {
        // given
        Projectile projectile = mock(Projectile.class);
        Player shooter = mock(Player.class);
        given(projectile.getShooter()).willReturn(shooter);
        ProjectileLaunchEvent event = new ProjectileLaunchEvent(projectile);
        given(listenerService.shouldCancelEvent(shooter)).willReturn(true);

        // when
        listener.onProjectileLaunch(event);

        // then
        verify(listenerService).shouldCancelEvent(shooter);
        assertThat(event.isCancelled(), equalTo(true));
    }

    @Test
    public void shouldCancelEntityTargetEvent() {
        // given
        EntityTargetEvent event = mock(EntityTargetEvent.class);
        Entity target = mock(Entity.class);
        given(event.getTarget()).willReturn(target);
        given(listenerService.shouldCancelEvent(target)).willReturn(true);

        // when
        listener.onEntityTarget(event);

        // then
        verify(listenerService).shouldCancelEvent(target);
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldNotCancelEntityTargetEvent() {
        // given
        EntityTargetEvent event = mock(EntityTargetEvent.class);
        Entity target = mock(Entity.class);
        given(event.getTarget()).willReturn(target);
        given(listenerService.shouldCancelEvent(target)).willReturn(false);

        // when
        listener.onEntityTarget(event);

        // then
        verify(listenerService).shouldCancelEvent(target);
        verify(event, only()).getTarget();
    }
}
