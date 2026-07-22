package com.example.pongglyphtoy.glyph

import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Test

class PaddleFrameRendererTest {

    @Test
    fun paddlesStayOnTheirOuterHalfOfMatrix() {
        listOf(-1f, 0f, 1f).forEach { position ->
            val frame = PaddleFrameRenderer.render(position)
            val litPixels = frame.indices.filter { frame[it] > 0 }

            assertTrue(litPixels.isNotEmpty())
            assertTrue(litPixels.any { it / MATRIX_SIZE < MATRIX_CENTER })
            assertTrue(litPixels.any { it / MATRIX_SIZE > MATRIX_CENTER })
            litPixels.forEach { index ->
                val x = index % MATRIX_SIZE
                val y = index / MATRIX_SIZE
                assertTrue(hypot((x - MATRIX_CENTER).toDouble(), (y - MATRIX_CENTER).toDouble()) >= 10.0)
            }
        }
    }

    @Test
    fun positionMovesPaddleFromLeftToRight() {
        val leftAverageX = averagePlayerPaddleX(PaddleFrameRenderer.render(-1f))
        val centerAverageX = averagePlayerPaddleX(PaddleFrameRenderer.render(0f))
        val rightAverageX = averagePlayerPaddleX(PaddleFrameRenderer.render(1f))

        assertTrue(leftAverageX < centerAverageX)
        assertTrue(centerAverageX < rightAverageX)
    }

    @Test
    fun opponentPositionMovesTopPaddleFromLeftToRight() {
        val leftAverageX = averageOpponentPaddleX(
            PaddleFrameRenderer.render(playerPosition = 0f, opponentPosition = -1f)
        )
        val rightAverageX = averageOpponentPaddleX(
            PaddleFrameRenderer.render(playerPosition = 0f, opponentPosition = 1f)
        )

        assertTrue(leftAverageX < rightAverageX)
    }

    @Test
    fun playerOnlyFrameCanBeUsedAsMenuScrollbar() {
        val frame = PaddleFrameRenderer.renderPlayerPaddle(0f)
        val litPixels = frame.indices.filter { frame[it] > 0 }

        assertTrue(litPixels.isNotEmpty())
        assertTrue(litPixels.all { it / MATRIX_SIZE >= MATRIX_CENTER })
    }

    private fun averagePlayerPaddleX(frame: IntArray): Double {
        val xValues = frame.indices
            .filter { frame[it] > 0 && it / MATRIX_SIZE > MATRIX_CENTER }
            .map { it % MATRIX_SIZE }
        return xValues.average()
    }

    private fun averageOpponentPaddleX(frame: IntArray): Double {
        val xValues = frame.indices
            .filter { frame[it] > 0 && it / MATRIX_SIZE < MATRIX_CENTER }
            .map { it % MATRIX_SIZE }
        return xValues.average()
    }

    private companion object {
        private const val MATRIX_SIZE = 25
        private const val MATRIX_CENTER = 12
    }
}
