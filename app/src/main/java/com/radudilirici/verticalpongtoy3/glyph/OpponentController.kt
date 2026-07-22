package com.radudilirici.verticalpongtoy3.glyph

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

internal class OpponentController(
    private val randomUnit: () -> Float = { Random.Default.nextFloat() }
) {

    private var settings = GameDifficulty.EASY.opponentSettings
    private var position = 0f
    private var velocity = 0f
    private var targetPosition = 0f
    private var reactionRemainingSeconds = 0f
    private var wasTrackingBall = false

    fun reset() {
        position = 0f
        velocity = 0f
        targetPosition = 0f
        reactionRemainingSeconds = 0f
        wasTrackingBall = false
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        settings = difficulty.opponentSettings
    }

    fun update(
        elapsedSeconds: Float,
        ballX: Float,
        ballY: Float,
        ballVelocityX: Float,
        ballVelocityY: Float,
        shouldTrackBall: Boolean
    ): Float {
        val elapsed = elapsedSeconds.coerceIn(0f, MAX_UPDATE_SECONDS)

        if (shouldTrackBall && !wasTrackingBall) {
            reactionRemainingSeconds = randomRange(
                settings.minimumInitialReactionSeconds,
                settings.maximumInitialReactionSeconds
            )
        } else if (!shouldTrackBall) {
            targetPosition = 0f
        }
        wasTrackingBall = shouldTrackBall

        reactionRemainingSeconds -= elapsed
        if (shouldTrackBall && reactionRemainingSeconds <= 0f) {
            val predictedPosition = predictInterceptPosition(
                ballX,
                ballY,
                ballVelocityX,
                ballVelocityY
            )
            val aimError = randomRange(
                -settings.maximumAimError,
                settings.maximumAimError
            )
            targetPosition = (predictedPosition + aimError).coerceIn(-1f, 1f)
            reactionRemainingSeconds = randomRange(
                settings.minimumCorrectionIntervalSeconds,
                settings.maximumCorrectionIntervalSeconds
            )
        }

        val error = targetPosition - position
        val desiredVelocity = (error * settings.trackingGain)
            .coerceIn(
                -settings.maximumPositionSpeed,
                settings.maximumPositionSpeed
            )
        val maxVelocityChange = settings.maximumPositionAcceleration * elapsed
        velocity += (desiredVelocity - velocity)
            .coerceIn(-maxVelocityChange, maxVelocityChange)

        val nextPosition = position + velocity * elapsed
        val crossesTarget = error * (targetPosition - nextPosition) <= 0f
        if (crossesTarget) {
            position = targetPosition
            velocity = 0f
        } else {
            position = nextPosition.coerceIn(-1f, 1f)
        }
        return position
    }

    private fun predictInterceptPosition(
        ballX: Float,
        ballY: Float,
        ballVelocityX: Float,
        ballVelocityY: Float
    ): Float {
        val speed = hypot(ballVelocityX, ballVelocityY)
        if (speed <= MIN_BALL_SPEED || ballVelocityY >= 0f) {
            return 0f
        }

        val directionX = ballVelocityX / speed
        val directionY = ballVelocityY / speed
        val offsetX = ballX - MATRIX_CENTER
        val offsetY = ballY - MATRIX_CENTER
        val positionAlongDirection = offsetX * directionX + offsetY * directionY
        val discriminant = positionAlongDirection * positionAlongDirection +
            TRACKING_RADIUS * TRACKING_RADIUS -
            offsetX * offsetX -
            offsetY * offsetY
        val distance = -positionAlongDirection + sqrt(discriminant.coerceAtLeast(0f))
        val intersectionX = offsetX + directionX * distance
        val intersectionY = offsetY + directionY * distance
        val angle = normalizeDegrees(
            atan2(intersectionY, intersectionX) * RADIANS_TO_DEGREES
        )
        val angleOffset = signedAngleDifference(
            angle,
            PaddleFrameRenderer.ENEMY_CENTER_ANGLE_DEGREES
        )
        return (angleOffset / PaddleFrameRenderer.MAX_TRAVEL_DEGREES)
            .coerceIn(-1f, 1f)
    }

    private fun randomRange(minimum: Float, maximum: Float): Float =
        minimum + (maximum - minimum) * randomUnit().coerceIn(0f, 1f)

    private fun normalizeDegrees(value: Float): Float {
        var normalized = value % FULL_CIRCLE_DEGREES
        if (normalized < 0f) {
            normalized += FULL_CIRCLE_DEGREES
        }
        return normalized
    }

    private fun signedAngleDifference(angle: Float, reference: Float): Float {
        var difference = normalizeDegrees(angle) - normalizeDegrees(reference)
        if (difference > HALF_CIRCLE_DEGREES) {
            difference -= FULL_CIRCLE_DEGREES
        } else if (difference < -HALF_CIRCLE_DEGREES) {
            difference += FULL_CIRCLE_DEGREES
        }
        return difference
    }

    internal fun targetPositionForTest(): Float = targetPosition

    private companion object {
        private const val MATRIX_CENTER = 12f
        private const val TRACKING_RADIUS = 11.2f
        private const val MIN_BALL_SPEED = 0.001f
        private const val MAX_UPDATE_SECONDS = 0.05f
        private const val FULL_CIRCLE_DEGREES = 360f
        private const val HALF_CIRCLE_DEGREES = 180f
        private val RADIANS_TO_DEGREES = (180.0 / Math.PI).toFloat()
    }
}
