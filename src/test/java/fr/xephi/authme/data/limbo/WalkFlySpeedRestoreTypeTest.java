package fr.xephi.authme.data.limbo;

import org.bukkit.entity.Player;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.data.limbo.LimboPlayer.DEFAULT_FLY_SPEED;
import static fr.xephi.authme.data.limbo.LimboPlayer.DEFAULT_WALK_SPEED;
import static fr.xephi.authme.data.limbo.WalkFlySpeedRestoreType.DEFAULT;
import static fr.xephi.authme.data.limbo.WalkFlySpeedRestoreType.MAX_RESTORE;
import static fr.xephi.authme.data.limbo.WalkFlySpeedRestoreType.RESTORE;
import static fr.xephi.authme.data.limbo.WalkFlySpeedRestoreType.RESTORE_NO_ZERO;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link WalkFlySpeedRestoreType}.
 */
class WalkFlySpeedRestoreTypeTest {

    @ParameterizedTest
    @MethodSource("buildParams")
    void shouldRestoreToExpectedValue(TestParameters parameters) {
        // given
        LimboPlayer limbo = mock(LimboPlayer.class);
        given(limbo.getWalkSpeed()).willReturn(parameters.givenLimboWalkSpeed);
        given(limbo.getFlySpeed()).willReturn(parameters.givenLimboFlySpeed);

        Player player = mock(Player.class);
        given(player.getWalkSpeed()).willReturn(parameters.givenPlayerWalkSpeed);
        given(player.getFlySpeed()).willReturn(parameters.givenPlayerFlySpeed);

        // when
        parameters.testedType.restoreWalkSpeed(player, limbo);
        parameters.testedType.restoreFlySpeed(player, limbo);

        // then
        verify(player).setWalkSpeed(parameters.expectedWalkSpeed);
        verify(player).setFlySpeed(parameters.expectedFlySpeed);
    }

    private static List<TestParameters> buildParams() {
        return Arrays.asList(
            create(RESTORE).withLimbo(0.1f, 0.4f).withPlayer(0.3f, 0.9f).expect(0.1f, 0.4f),
            create(RESTORE).withLimbo(0.9f, 0.2f).withPlayer(0.3f, 0.0f).expect(0.9f, 0.2f),
            create(MAX_RESTORE).withLimbo(0.3f, 0.8f).withPlayer(0.5f, 0.2f).expect(0.5f, 0.8f),
            create(MAX_RESTORE).withLimbo(0.4f, 0.2f).withPlayer(0.1f, 0.4f).expect(0.4f, 0.4f),
            create(RESTORE_NO_ZERO).withLimbo(0.1f, 0.2f).withPlayer(0.5f, 0.1f).expect(0.1f, 0.2f),
            create(RESTORE_NO_ZERO).withLimbo(0.0f, 0.005f).withPlayer(0.4f, 0.8f).expect(DEFAULT_WALK_SPEED, DEFAULT_FLY_SPEED),
            create(DEFAULT).withLimbo(0.1f, 0.7f).withPlayer(0.4f, 0.0f).expect(DEFAULT_WALK_SPEED, DEFAULT_FLY_SPEED)
        );
    }

    private static TestParameters create(WalkFlySpeedRestoreType testedType) {
        TestParameters params = new TestParameters();
        params.testedType = testedType;
        return params;
    }

    private static final class TestParameters {
        private WalkFlySpeedRestoreType testedType;
        private float givenLimboWalkSpeed;
        private float givenLimboFlySpeed;
        private float givenPlayerWalkSpeed;
        private float givenPlayerFlySpeed;
        private float expectedWalkSpeed;
        private float expectedFlySpeed;
        
        TestParameters withLimbo(float walkSpeed, float flySpeed) {
            this.givenLimboWalkSpeed = walkSpeed;
            this.givenLimboFlySpeed = flySpeed;
            return this;
        }
        
        TestParameters withPlayer(float walkSpeed, float flySpeed) {
            this.givenPlayerWalkSpeed = walkSpeed;
            this.givenPlayerFlySpeed = flySpeed;
            return this;
        }
        
        TestParameters expect(float walkSpeed, float flySpeed) {
            this.expectedWalkSpeed = walkSpeed;
            this.expectedFlySpeed = flySpeed;
            return this;
        }

        @Override
        public String toString() {
            return testedType + " {" + expectedWalkSpeed + ", " + expectedFlySpeed + "}";
        }
    }
}
