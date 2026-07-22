package com.example.pongglyphtoy.glyph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal object PaddleFrameRenderer {

    fun render(
        playerPosition: Float,
        opponentPosition: Float = 0f
    ): IntArray {
        val frame = IntArray(MATRIX_SIZE * MATRIX_SIZE)
        drawPaddle(
            frame = frame,
            centerAngle = playerCenterAngle(playerPosition),
            allowedY = MATRIX_CENTER until MATRIX_SIZE
        )
        drawPaddle(
            frame = frame,
            centerAngle = enemyCenterAngle(opponentPosition),
            allowedY = 0..MATRIX_CENTER
        )
        return frame
    }

    fun renderPlayerPaddle(playerPosition: Float): IntArray {
        val frame = IntArray(MATRIX_SIZE * MATRIX_SIZE)
        drawPaddle(
            frame = frame,
            centerAngle = playerCenterAngle(playerPosition),
            allowedY = MATRIX_CENTER until MATRIX_SIZE
        )
        return frame
    }

    fun playerCenterAngle(position: Float): Float =
        PLAYER_CENTER_ANGLE_DEGREES - position.coerceIn(-1f, 1f) * MAX_TRAVEL_DEGREES

    fun enemyCenterAngle(position: Float): Float =
        ENEMY_CENTER_ANGLE_DEGREES + position.coerceIn(-1f, 1f) * MAX_TRAVEL_DEGREES

    private fun drawPaddle(
        frame: IntArray,
        centerAngle: Float,
        allowedY: IntRange
    ) {
        for (sample in 0 until PADDLE_SAMPLES) {
            val progress = sample.toFloat() / (PADDLE_SAMPLES - 1)
            val angleDegrees = centerAngle - PADDLE_HALF_ANGLE_DEGREES +
                progress * PADDLE_HALF_ANGLE_DEGREES * 2f
            val angleRadians = angleDegrees * PI.toFloat() / 180f

            val x = (MATRIX_CENTER + PADDLE_RADIUS * cos(angleRadians)).roundToInt()
            val y = (MATRIX_CENTER + PADDLE_RADIUS * sin(angleRadians)).roundToInt()
            if (x in 0 until MATRIX_SIZE && y in allowedY) {
                frame[y * MATRIX_SIZE + x] = MAX_BRIGHTNESS
            }
        }
    }

    internal const val MATRIX_SIZE = 25
    internal const val MATRIX_CENTER = 12
    internal const val PLAYER_CENTER_ANGLE_DEGREES = 90f
    internal const val ENEMY_CENTER_ANGLE_DEGREES = 270f
    internal const val MAX_TRAVEL_DEGREES = 55f
    internal const val PADDLE_HALF_ANGLE_DEGREES = 14f

    private const val PADDLE_SAMPLES = 9
    private const val MAX_BRIGHTNESS = 2047
    private const val PADDLE_RADIUS = 12f
}
