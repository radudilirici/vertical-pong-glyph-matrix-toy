package com.radudilirici.verticalpongtoy3.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PongFrameRendererTest {

    @Test
    fun playingFrameContainsTwoByTwoBallAndBothPaddles() {
        val frame = PongFrameRenderer.render(
            PongGameSnapshot(
                phase = GamePhase.PLAYING,
                playerPaddlePosition = 0f,
                ballX = 12f,
                ballY = 12f,
                opponentScore = 0,
                playerScore = 0
            )
        )

        assertEquals(4, litCount(frame, left = 12, top = 12, width = 2, height = 2))
        assertTrue(frame.indices.any { frame[it] > 0 && it / MATRIX_SIZE < 3 })
        assertTrue(frame.indices.any { frame[it] > 0 && it / MATRIX_SIZE > 21 })
    }

    @Test
    fun scoreFramePlacesOpponentOnTopAndPlayerOnBottom() {
        val frame = PongFrameRenderer.render(
            PongGameSnapshot(
                phase = GamePhase.SHOWING_SCORE,
                playerPaddlePosition = 0f,
                ballX = 12f,
                ballY = 12f,
                opponentScore = 0,
                playerScore = 1
            )
        )

        assertTrue(frame.indices.any { frame[it] > 0 && it / MATRIX_SIZE in 3..7 })
        assertTrue(frame.indices.any { frame[it] > 0 && it / MATRIX_SIZE in 17..21 })
        assertTrue(frame.indices.none { frame[it] > 0 && it / MATRIX_SIZE in 9..15 })
    }

    @Test
    fun scoreFrameDisplaysSetInsteadOfTen() {
        val frame = PongFrameRenderer.render(
            PongGameSnapshot(
                phase = GamePhase.SHOWING_SCORE,
                playerPaddlePosition = 0f,
                ballX = 12f,
                ballY = 12f,
                opponentScore = 10,
                playerScore = 10
            )
        )

        assertEquals(7, minimumLitX(frame, rows = 3..7))
        assertEquals(17, maximumLitX(frame, rows = 3..7))
        assertEquals(7, minimumLitX(frame, rows = 17..21))
        assertEquals(17, maximumLitX(frame, rows = 17..21))
    }

    @Test
    fun matchEndFrameShowsPlayerWinMessageOnTwoRows() {
        val frame = PongFrameRenderer.render(
            PongGameSnapshot(
                phase = GamePhase.MATCH_OVER,
                playerPaddlePosition = 0f,
                ballX = 12f,
                ballY = 12f,
                opponentScore = 5,
                playerScore = 11
            )
        )

        assertTrue(frame.indices.any { frame[it] > 0 && it / MATRIX_SIZE in 5..9 })
        assertTrue(frame.indices.any { frame[it] > 0 && it / MATRIX_SIZE in 15..19 })
        assertTrue(frame.indices.none { frame[it] > 0 && it / MATRIX_SIZE in 10..14 })
    }

    @Test
    fun matchEndFrameShowsWiderNextTimeMessageAfterLoss() {
        val frame = PongFrameRenderer.render(
            PongGameSnapshot(
                phase = GamePhase.MATCH_OVER,
                playerPaddlePosition = 0f,
                ballX = 12f,
                ballY = 12f,
                opponentScore = 11,
                playerScore = 5
            )
        )

        assertEquals(5, minimumLitX(frame, rows = 5..9))
        assertEquals(19, maximumLitX(frame, rows = 5..9))
        assertEquals(5, minimumLitX(frame, rows = 15..19))
        assertEquals(19, maximumLitX(frame, rows = 15..19))
    }

    @Test
    fun menuScrollsSelectedDifficultyToCenterAndShowsBottomScrollbar() {
        val easyFrame = PongFrameRenderer.render(
            menuSnapshot(GameDifficulty.EASY, position = -1f)
        )
        val mediumFrame = PongFrameRenderer.render(
            menuSnapshot(GameDifficulty.MEDIUM, position = 0f)
        )
        val hardFrame = PongFrameRenderer.render(
            menuSnapshot(GameDifficulty.HARD, position = 1f)
        )

        assertTrue(maxBrightnessInRegion(easyFrame, 6..18, 8..12) > 1000)
        assertTrue(maxBrightnessInRegion(mediumFrame, 6..18, 8..12) > 1000)
        assertTrue(maxBrightnessInRegion(hardFrame, 6..18, 8..12) > 1000)
        assertTrue(maxBrightnessInRegion(mediumFrame, 0..5, 8..12) in 1..1000)
        assertTrue(maxBrightnessInRegion(mediumFrame, 19..24, 8..12) in 1..1000)
        assertTrue(averageBottomX(easyFrame) < averageBottomX(mediumFrame))
        assertTrue(averageBottomX(mediumFrame) < averageBottomX(hardFrame))
    }

    @Test
    fun menuShowsDummyBallButStartAnimationDoesNot() {
        val menuFrame = PongFrameRenderer.render(
            menuSnapshot(GameDifficulty.MEDIUM, position = 0f)
        )
        val animationFrame = PongFrameRenderer.render(
            animationSnapshot(progress = 0.1f)
        )

        assertEquals(4, litCount(menuFrame, left = 12, top = 3, width = 2, height = 2))
        assertEquals(0, litCount(animationFrame, left = 12, top = 3, width = 2, height = 2))
    }

    @Test
    fun startAnimationGrowsThenShrinksToTwoByTwoBall() {
        val small = PongFrameRenderer.render(animationSnapshot(0.1f))
        val large = PongFrameRenderer.render(animationSnapshot(0.5f))
        val final = PongFrameRenderer.render(animationSnapshot(0.95f))
        val smallBallPixels = litCount(small, left = 8, top = 6, width = 10, height = 10)
        val largeBallPixels = litCount(large, left = 8, top = 6, width = 10, height = 10)
        val finalBallPixels = litCount(final, left = 8, top = 6, width = 10, height = 10)

        assertTrue(largeBallPixels > smallBallPixels)
        assertTrue(largeBallPixels > finalBallPixels)
        assertTrue(largeBallPixels > 45)
        assertEquals(4, finalBallPixels)
    }

    private fun menuSnapshot(
        difficulty: GameDifficulty,
        position: Float
    ) = PongGameSnapshot(
        phase = GamePhase.MENU,
        playerPaddlePosition = position,
        ballX = 12f,
        ballY = 10f,
        opponentScore = 0,
        playerScore = 0,
        selectedDifficulty = difficulty
    )

    private fun animationSnapshot(progress: Float) = PongGameSnapshot(
        phase = GamePhase.STARTING_ANIMATION,
        playerPaddlePosition = 0f,
        ballX = 12f,
        ballY = 10f,
        opponentScore = 0,
        playerScore = 0,
        startAnimationProgress = progress
    )

    private fun maxBrightnessInRegion(
        frame: IntArray,
        columns: IntRange,
        rows: IntRange
    ): Int =
        frame.indices
            .filter {
                it % MATRIX_SIZE in columns && it / MATRIX_SIZE in rows
            }
            .maxOf { frame[it] }

    private fun averageBottomX(frame: IntArray): Double =
        frame.indices
            .filter { frame[it] > 1000 && it / MATRIX_SIZE > 20 }
            .map { it % MATRIX_SIZE }
            .average()

    private fun minimumLitX(frame: IntArray, rows: IntRange): Int =
        frame.indices
            .filter { frame[it] > 0 && it / MATRIX_SIZE in rows }
            .minOf { it % MATRIX_SIZE }

    private fun maximumLitX(frame: IntArray, rows: IntRange): Int =
        frame.indices
            .filter { frame[it] > 0 && it / MATRIX_SIZE in rows }
            .maxOf { it % MATRIX_SIZE }

    private fun litCount(
        frame: IntArray,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ): Int {
        var count = 0
        for (y in top until top + height) {
            for (x in left until left + width) {
                if (frame[y * MATRIX_SIZE + x] > 0) {
                    count += 1
                }
            }
        }
        return count
    }

    private companion object {
        private const val MATRIX_SIZE = 25
    }
}
