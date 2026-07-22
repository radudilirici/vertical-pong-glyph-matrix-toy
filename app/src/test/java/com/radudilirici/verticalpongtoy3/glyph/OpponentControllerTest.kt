package com.radudilirici.verticalpongtoy3.glyph

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpponentControllerTest {

    @Test
    fun waitsBeforeReactingToIncomingBall() {
        val controller = OpponentController { 0.5f }
        controller.reset()

        repeat(4) {
            controller.update(
                elapsedSeconds = 0.05f,
                ballX = 12f,
                ballY = 12f,
                ballVelocityX = 4f,
                ballVelocityY = -6f,
                shouldTrackBall = true
            )
        }

        assertEquals(0f, controller.targetPositionForTest(), 0.001f)
    }

    @Test
    fun followsPredictedInterceptWithoutTeleporting() {
        val controller = OpponentController { 0.5f }
        controller.reset()

        var position = 0f
        repeat(12) {
            position = controller.update(
                elapsedSeconds = 0.05f,
                ballX = 12f,
                ballY = 12f,
                ballVelocityX = 4f,
                ballVelocityY = -6f,
                shouldTrackBall = true
            )
        }

        assertTrue(position > 0f)
        assertTrue(position < controller.targetPositionForTest())
        assertTrue(position < 0.35f)
    }

    @Test
    fun aimingErrorChangesTrackingTarget() {
        val aimLeft = OpponentController { 0f }
        val aimRight = OpponentController { 1f }

        repeat(20) {
            aimLeft.update(0.05f, 12f, 12f, 0f, -7f, true)
            aimRight.update(0.05f, 12f, 12f, 0f, -7f, true)
        }

        assertTrue(
            abs(
                aimLeft.targetPositionForTest() -
                    aimRight.targetPositionForTest()
            ) >= 0.3f
        )
    }

    @Test
    fun returnsTowardCenterWhenBallMovesAway() {
        val controller = OpponentController { 0.5f }
        repeat(30) {
            controller.update(0.05f, 12f, 12f, 5f, -5f, true)
        }
        val trackedPosition = controller.update(0.05f, 12f, 12f, 0f, 7f, false)

        repeat(20) {
            controller.update(0.05f, 12f, 12f, 0f, 7f, false)
        }

        assertTrue(abs(controller.targetPositionForTest()) < 0.001f)
        assertTrue(
            abs(
                controller.update(0.05f, 12f, 12f, 0f, 7f, false)
            ) < abs(trackedPosition)
        )
    }

    @Test
    fun hardDifficultyRespondsFasterThanMedium() {
        val medium = OpponentController { 0.5f }
        medium.setDifficulty(GameDifficulty.MEDIUM)
        val hard = OpponentController { 0.5f }
        hard.setDifficulty(GameDifficulty.HARD)

        var mediumPosition = 0f
        var hardPosition = 0f
        repeat(12) {
            mediumPosition = medium.update(0.05f, 12f, 12f, 4f, -6f, true)
            hardPosition = hard.update(0.05f, 12f, 12f, 4f, -6f, true)
        }

        assertTrue(hardPosition > mediumPosition)
    }

}
