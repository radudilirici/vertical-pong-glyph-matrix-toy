package com.radudilirici.verticalpongtoy3.glyph

import kotlin.math.roundToInt

internal object PongFrameRenderer {

    fun render(snapshot: PongGameSnapshot): IntArray =
        when (snapshot.phase) {
            GamePhase.MENU -> renderMenu(snapshot)
            GamePhase.STARTING_ANIMATION ->
                renderStartAnimation(snapshot)
            GamePhase.PLAYING -> renderPlayingFrame(snapshot)
            GamePhase.SHOWING_SCORE ->
                renderScore(snapshot.opponentScore, snapshot.playerScore)
            GamePhase.MATCH_OVER ->
                renderMatchResult(snapshot.playerScore > snapshot.opponentScore)
        }

    private fun renderMenu(snapshot: PongGameSnapshot): IntArray {
        val frame = renderMenuBackground(
            playerPaddlePosition = snapshot.playerPaddlePosition,
            selectedDifficulty = snapshot.selectedDifficulty
        )
        drawBall(frame, snapshot.menuBallX, snapshot.menuBallY)
        return frame
    }

    internal fun renderMenuBackground(
        playerPaddlePosition: Float,
        selectedDifficulty: GameDifficulty
    ): IntArray {
        val frame = PaddleFrameRenderer.renderPlayerPaddle(
            playerPaddlePosition
        )
        drawMenuOption(
            frame,
            "EASY",
            -1f,
            GameDifficulty.EASY,
            playerPaddlePosition,
            selectedDifficulty
        )
        drawMenuOption(
            frame,
            "MED",
            0f,
            GameDifficulty.MEDIUM,
            playerPaddlePosition,
            selectedDifficulty
        )
        drawMenuOption(
            frame,
            "HARD",
            1f,
            GameDifficulty.HARD,
            playerPaddlePosition,
            selectedDifficulty
        )
        return frame
    }

    private fun drawMenuOption(
        frame: IntArray,
        text: String,
        optionPosition: Float,
        difficulty: GameDifficulty,
        playerPaddlePosition: Float,
        selectedDifficulty: GameDifficulty
    ) {
        drawHorizontalText(
            frame = frame,
            text = text,
            centerX = MATRIX_CENTER + (
                optionPosition - playerPaddlePosition
                ) * MENU_ITEM_SPACING,
            top = MENU_TEXT_Y,
            letterSpacing = LETTER_SPACING,
            brightness = if (selectedDifficulty == difficulty) {
                MAX_BRIGHTNESS
            } else {
                MENU_DIM_BRIGHTNESS
            }
        )
    }

    private fun renderStartAnimation(snapshot: PongGameSnapshot): IntArray {
        val frame = PaddleFrameRenderer.render(
            playerPosition = snapshot.playerPaddlePosition,
            opponentPosition = snapshot.opponentPaddlePosition
        )
        val normalized = snapshot.startAnimationProgress.coerceIn(0f, 1f)
        val pattern = when {
            normalized < 0.08f -> BALL_PATTERN_2
            normalized < 0.19f -> BALL_PATTERN_4
            normalized < 0.35f -> BALL_PATTERN_6
            normalized < 0.65f -> BALL_PATTERN_8
            normalized < 0.83f -> BALL_PATTERN_6
            normalized < 0.92f -> BALL_PATTERN_4
            else -> BALL_PATTERN_2
        }
        val size = pattern.size
        val left = SERVE_CENTER_X - size / 2 + 1
        val top = SERVE_CENTER_Y - size / 2 + 1

        pattern.forEachIndexed { row, bits ->
            for (column in 0 until size) {
                val mask = 1 shl (size - column - 1)
                if (bits and mask != 0) {
                    val x = left + column
                    val y = top + row
                    frame[y * MATRIX_SIZE + x] = MAX_BRIGHTNESS
                }
            }
        }
        return frame
    }

    private fun renderPlayingFrame(snapshot: PongGameSnapshot): IntArray {
        val frame = PaddleFrameRenderer.render(
            playerPosition = snapshot.playerPaddlePosition,
            opponentPosition = snapshot.opponentPaddlePosition
        )
        drawBall(frame, snapshot.ballX, snapshot.ballY)
        return frame
    }

    private fun drawBall(frame: IntArray, centerX: Float, centerY: Float) {
        val ballLeft = (centerX - BALL_CENTER_OFFSET).roundToInt()
        val ballTop = (centerY - BALL_CENTER_OFFSET).roundToInt()

        for (y in ballTop until ballTop + BALL_SIZE) {
            for (x in ballLeft until ballLeft + BALL_SIZE) {
                if (x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE) {
                    frame[y * MATRIX_SIZE + x] = MAX_BRIGHTNESS
                }
            }
        }
    }

    private fun renderScore(opponentScore: Int, playerScore: Int): IntArray {
        val frame = IntArray(MATRIX_SIZE * MATRIX_SIZE)
        drawNumber(frame, opponentScore, TOP_SCORE_Y)
        drawNumber(frame, playerScore, BOTTOM_SCORE_Y)
        return frame
    }

    private fun renderMatchResult(playerWon: Boolean): IntArray {
        val frame = IntArray(MATRIX_SIZE * MATRIX_SIZE)
        val topText = if (playerWon) "YOU" else "NEXT"
        val bottomText = if (playerWon) "WIN" else "TIME"
        drawHorizontalText(
            frame,
            topText,
            MATRIX_CENTER,
            MATCH_TOP_TEXT_Y,
            LETTER_SPACING,
            MAX_BRIGHTNESS
        )
        drawHorizontalText(
            frame,
            bottomText,
            MATRIX_CENTER,
            MATCH_BOTTOM_TEXT_Y,
            LETTER_SPACING,
            MAX_BRIGHTNESS
        )
        return frame
    }

    private fun drawNumber(frame: IntArray, score: Int, top: Int) {
        val digits = score.coerceIn(0, MAX_DISPLAY_SCORE)
            .toString()
            .map { it.digitToInt() }
        val width = digits.size * DIGIT_WIDTH + (digits.size - 1) * DIGIT_SPACING
        val startX = (MATRIX_SIZE - width) / 2

        digits.forEachIndexed { index, digit ->
            val digitX = startX + index * (DIGIT_WIDTH + DIGIT_SPACING)
            DIGIT_ROWS[digit].forEachIndexed { row, bits ->
                for (column in 0 until DIGIT_WIDTH) {
                    val mask = 1 shl (DIGIT_WIDTH - column - 1)
                    if (bits and mask != 0) {
                        val x = digitX + column
                        val y = top + row
                        frame[y * MATRIX_SIZE + x] = MAX_BRIGHTNESS
                    }
                }
            }
        }
    }

    private fun drawHorizontalText(
        frame: IntArray,
        text: String,
        centerX: Float,
        top: Int,
        letterSpacing: Int,
        brightness: Int
    ) {
        val textWidth = text.length * LETTER_WIDTH +
            (text.length - 1) * letterSpacing
        val left = centerX - textWidth / 2f

        text.forEachIndexed { characterIndex, character ->
            val rows = rowsForCharacter(character)
            rows.forEachIndexed { row, bits ->
                for (column in 0 until LETTER_WIDTH) {
                    val mask = 1 shl (LETTER_WIDTH - column - 1)
                    if (bits and mask != 0) {
                        val x = (
                            left + characterIndex * (
                                LETTER_WIDTH + letterSpacing
                                ) + column
                            ).roundToInt()
                        val y = top + row
                        if (x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE) {
                            frame[y * MATRIX_SIZE + x] = brightness
                        }
                    }
                }
            }
        }
    }

    private fun rowsForCharacter(character: Char): IntArray =
        when (character) {
            'A' -> intArrayOf(0b010, 0b101, 0b111, 0b101, 0b101)
            'D' -> intArrayOf(0b110, 0b101, 0b101, 0b101, 0b110)
            'E' -> intArrayOf(0b111, 0b100, 0b110, 0b100, 0b111)
            'G' -> intArrayOf(0b111, 0b100, 0b101, 0b101, 0b111)
            'H' -> intArrayOf(0b101, 0b101, 0b111, 0b101, 0b101)
            'I' -> intArrayOf(0b111, 0b010, 0b010, 0b010, 0b111)
            'L' -> intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111)
            'M' -> intArrayOf(0b101, 0b111, 0b111, 0b101, 0b101)
            'N' -> intArrayOf(0b111, 0b101, 0b101, 0b101, 0b101)
            'O' -> DIGIT_ROWS[0]
            'P' -> intArrayOf(0b111, 0b101, 0b111, 0b100, 0b100)
            'R' -> intArrayOf(0b110, 0b101, 0b110, 0b101, 0b101)
            'S' -> intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111)
            'T' -> intArrayOf(0b111, 0b010, 0b010, 0b010, 0b010)
            'U' -> intArrayOf(0b101, 0b101, 0b101, 0b101, 0b111)
            'V' -> intArrayOf(0b101, 0b101, 0b101, 0b101, 0b010)
            'W' -> intArrayOf(0b101, 0b101, 0b111, 0b111, 0b101)
            'X' -> intArrayOf(0b101, 0b101, 0b010, 0b101, 0b101)
            'Y' -> intArrayOf(0b101, 0b101, 0b010, 0b010, 0b010)
            else -> intArrayOf(0, 0, 0, 0, 0)
        }

    private const val MATRIX_SIZE = 25
    private const val MATRIX_CENTER = 12f
    private const val SERVE_CENTER_X = 12
    private const val SERVE_CENTER_Y = MATRIX_CENTER.toInt() - 5
    private const val BALL_SIZE = 2
    private const val BALL_CENTER_OFFSET = 0.5f
    private const val MAX_BRIGHTNESS = 2047
    private const val MENU_DIM_BRIGHTNESS = 320
    private const val MAX_DISPLAY_SCORE = 99
    private const val LETTER_WIDTH = 3
    private const val LETTER_SPACING = 1
    private const val DIGIT_WIDTH = 3
    private const val DIGIT_SPACING = 1
    private const val TOP_SCORE_Y = 3
    private const val BOTTOM_SCORE_Y = 17
    private const val MATCH_TOP_TEXT_Y = 5
    private const val MATCH_BOTTOM_TEXT_Y = 15
    private const val MENU_TEXT_Y = 8
    private const val MENU_ITEM_SPACING = 16f

    private val BALL_PATTERN_2 = intArrayOf(0b11, 0b11)
    private val BALL_PATTERN_4 = intArrayOf(
        0b0110,
        0b1111,
        0b1111,
        0b0110
    )
    private val BALL_PATTERN_6 = intArrayOf(
        0b011110,
        0b111111,
        0b111111,
        0b111111,
        0b111111,
        0b011110
    )
    private val BALL_PATTERN_8 = intArrayOf(
        0b00111100,
        0b01111110,
        0b11111111,
        0b11111111,
        0b11111111,
        0b11111111,
        0b01111110,
        0b00111100
    )

    private val DIGIT_ROWS = arrayOf(
        intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111),
        intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111),
        intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111),
        intArrayOf(0b111, 0b001, 0b010, 0b010, 0b010),
        intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111),
        intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111)
    )
}
