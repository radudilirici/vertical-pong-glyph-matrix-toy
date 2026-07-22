package com.example.pongglyphtoy.glyph

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaddleMotionControllerTest {

    @Test
    fun firstReadingCalibratesNeutralPosition() {
        val controller = PaddleMotionController()

        controller.updateRoll(0.3f)

        assertEquals(0f, controller.targetPositionForTest(), 0.001f)
    }

    @Test
    fun smallRollChangesAreFilteredByDeadZone() {
        val controller = PaddleMotionController()
        controller.updateRoll(0f)

        repeat(30) {
            controller.updateRoll(0.02f)
        }

        assertEquals(0f, controller.targetPositionForTest(), 0.001f)
    }

    @Test
    fun negativeRollMovesAndClampsPaddleTargetToLeft() {
        val controller = PaddleMotionController()
        controller.updateRoll(0f)

        repeat(50) {
            controller.updateRoll(-1f)
        }

        assertEquals(-1f, controller.targetPositionForTest(), 0.001f)
    }

    @Test
    fun frameUpdatesApproachTargetGradually() {
        val controller = PaddleMotionController()
        controller.updateRoll(0f)
        repeat(50) {
            controller.updateRoll(0.3f)
        }

        val firstFrame = controller.advanceFrame()
        val laterFrame = generateSequence { controller.advanceFrame() }
            .drop(20)
            .first()

        assertTrue(firstFrame in 0f..0.161f)
        assertTrue(laterFrame > firstFrame)
        assertTrue(laterFrame <= 1f)
    }

    @Test
    fun largerPositionErrorProducesFasterInitialMovement() {
        val smallMoveController = PaddleMotionController()
        smallMoveController.updateRoll(0f)
        repeat(50) {
            smallMoveController.updateRoll(0.08f)
        }

        val largeMoveController = PaddleMotionController()
        largeMoveController.updateRoll(0f)
        repeat(50) {
            largeMoveController.updateRoll(1f)
        }

        val smallStep = smallMoveController.advanceFrame()
        val largeStep = largeMoveController.advanceFrame()

        assertTrue(largeStep > smallStep * 2f)
    }

    @Test
    fun paddleDeceleratesAsItApproachesTarget() {
        val controller = PaddleMotionController()
        controller.updateRoll(0f)
        repeat(50) {
            controller.updateRoll(1f)
        }

        val positions = buildList {
            repeat(20) {
                add(controller.advanceFrame())
            }
        }
        val frameSpeeds = positions.zipWithNext { previous, current ->
            current - previous
        }.filter { it > 0f }

        assertTrue(frameSpeeds.max() > frameSpeeds.last())
    }

    @Test
    fun frameVelocityIsCappedAtPointTwelve() {
        val controller = PaddleMotionController()
        controller.updateRoll(0f)
        repeat(50) {
            controller.updateRoll(1f)
        }

        val positions = buildList {
            add(0f)
            repeat(30) {
                add(controller.advanceFrame())
            }
        }

        assertTrue(
            positions.zipWithNext { previous, current -> abs(current - previous) }
                .all { it <= 0.1201f }
        )
    }

}
