package com.radudilirici.verticalpongtoy3.glyph

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

internal class PaddleMotionController {

    private var neutralRoll: Float? = null
    private var filteredRollDelta = 0f
    private var targetPosition = 0f
    private var currentPosition = 0f
    private var velocity = 0f

    fun updateRoll(rollRadians: Float) {
        val neutral = neutralRoll
        if (neutral == null) {
            neutralRoll = rollRadians
            return
        }

        val rollDelta = normalizeRadians(rollRadians - neutral)
        filteredRollDelta += SENSOR_FILTER_ALPHA * (rollDelta - filteredRollDelta)

        val magnitude = abs(filteredRollDelta)
        targetPosition = if (magnitude <= ROLL_DEAD_ZONE_RADIANS) {
            0f
        } else {
            val usableRange = MAX_ROLL_RADIANS - ROLL_DEAD_ZONE_RADIANS
            val normalized = (magnitude - ROLL_DEAD_ZONE_RADIANS) / usableRange
            (filteredRollDelta.sign * normalized).coerceIn(-1f, 1f)
        }
    }

    fun advanceFrame(): Float {
        val error = targetPosition - currentPosition
        if (abs(error) <= POSITION_EPSILON && abs(velocity) <= VELOCITY_EPSILON) {
            currentPosition = targetPosition
            velocity = 0f
            return currentPosition
        }

        velocity = ((velocity + error * ERROR_ACCELERATION) * VELOCITY_DAMPING)
            .coerceIn(-MAX_FRAME_VELOCITY, MAX_FRAME_VELOCITY)

        val nextPosition = currentPosition + velocity
        val crossesTarget = error * (targetPosition - nextPosition) <= 0f
        if (crossesTarget) {
            currentPosition = targetPosition
            velocity = 0f
        } else {
            currentPosition = nextPosition.coerceIn(-1f, 1f)
        }

        return currentPosition
    }

    fun recalibrate() {
        neutralRoll = null
        filteredRollDelta = 0f
        targetPosition = 0f
        velocity = 0f
    }

    fun reset() {
        neutralRoll = null
        filteredRollDelta = 0f
        targetPosition = 0f
        currentPosition = 0f
        velocity = 0f
    }

    internal fun targetPositionForTest(): Float = targetPosition

    private fun normalizeRadians(value: Float): Float {
        var normalized = value
        val fullTurn = (2.0 * PI).toFloat()
        while (normalized > PI) {
            normalized -= fullTurn
        }
        while (normalized < -PI) {
            normalized += fullTurn
        }
        return normalized
    }

    private companion object {
        private const val SENSOR_FILTER_ALPHA = 0.12f
        private const val ROLL_DEAD_ZONE_RADIANS = 0.035f
        private const val MAX_ROLL_RADIANS = 0.45f
        private const val ERROR_ACCELERATION = 0.16f
        private const val VELOCITY_DAMPING = 0.72f
        private const val MAX_FRAME_VELOCITY = 0.08f
        private const val POSITION_EPSILON = 0.002f
        private const val VELOCITY_EPSILON = 0.001f
    }
}
