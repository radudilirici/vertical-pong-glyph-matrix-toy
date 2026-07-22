package com.example.pongglyphtoy.glyph

import kotlin.math.hypot

internal data class MenuBallState(
    val x: Float,
    val y: Float
)

internal class MenuBallController {

    private var x = START_X
    private var y = START_Y
    private var velocityX = START_VELOCITY_X
    private var velocityY = START_VELOCITY_Y

    fun reset() {
        x = START_X
        y = START_Y
        velocityX = START_VELOCITY_X
        velocityY = START_VELOCITY_Y
    }

    fun update(elapsedSeconds: Float) {
        var remaining = elapsedSeconds.coerceIn(0f, MAX_UPDATE_SECONDS)
        while (remaining > 0f) {
            val step = minOf(remaining, PHYSICS_STEP_SECONDS)
            advance(step)
            remaining -= step
        }
    }

    fun state(): MenuBallState = MenuBallState(x, y)

    private fun advance(step: Float) {
        var candidateX = x + velocityX * step
        var candidateY = y + velocityY * step
        val offsetX = candidateX - MATRIX_CENTER
        val offsetY = candidateY - MATRIX_CENTER
        val radius = hypot(offsetX, offsetY)

        if (radius > MAX_CENTER_RADIUS) {
            val normalX = offsetX / radius
            val normalY = offsetY / radius
            val velocityAlongNormal =
                velocityX * normalX + velocityY * normalY
            if (velocityAlongNormal > 0f) {
                velocityX -= 2f * velocityAlongNormal * normalX
                velocityY -= 2f * velocityAlongNormal * normalY
                candidateX = x + velocityX * step
                candidateY = y + velocityY * step
            }
        }

        if (candidateY > LOWER_CENTER_Y && velocityY > 0f) {
            velocityY = -velocityY
            candidateY = y + velocityY * step
        }

        x = candidateX
        y = candidateY
        keepInsideVisibleEdge()
    }

    private fun keepInsideVisibleEdge() {
        val offsetX = x - MATRIX_CENTER
        val offsetY = y - MATRIX_CENTER
        val radius = hypot(offsetX, offsetY)
        if (radius > MAX_CENTER_RADIUS) {
            x = MATRIX_CENTER + offsetX / radius * MAX_CENTER_RADIUS
            y = MATRIX_CENTER + offsetY / radius * MAX_CENTER_RADIUS
        }
        y = y.coerceAtMost(LOWER_CENTER_Y)
    }

    internal fun setStateForTest(
        x: Float,
        y: Float,
        velocityX: Float,
        velocityY: Float
    ) {
        this.x = x
        this.y = y
        this.velocityX = velocityX
        this.velocityY = velocityY
    }

    internal fun velocityForTest(): Pair<Float, Float> =
        velocityX to velocityY

    private companion object {
        private const val MATRIX_CENTER = 12f
        private const val MAX_CENTER_RADIUS = 11f
        private const val LOWER_CENTER_Y = 6.5f
        private const val START_X = 12f
        private const val START_Y = 3f
        private const val START_VELOCITY_X = 4.5f
        private const val START_VELOCITY_Y = 5.5f
        private const val PHYSICS_STEP_SECONDS = 0.008f
        private const val MAX_UPDATE_SECONDS = 0.05f
    }
}
