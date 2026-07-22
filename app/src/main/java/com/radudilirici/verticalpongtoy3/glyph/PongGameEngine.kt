package com.radudilirici.verticalpongtoy3.glyph

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal enum class GamePhase {
    MENU,
    STARTING_ANIMATION,
    PLAYING,
    SHOWING_SCORE,
    MATCH_OVER
}

internal data class PongGameSnapshot(
    val phase: GamePhase,
    val playerPaddlePosition: Float,
    val ballX: Float,
    val ballY: Float,
    val opponentScore: Int,
    val playerScore: Int,
    val opponentPaddlePosition: Float = 0f,
    val selectedDifficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val startAnimationProgress: Float = 0f,
    val menuBallX: Float = 12f,
    val menuBallY: Float = 3f
)

internal class PongGameEngine(
    private val opponentController: OpponentController = OpponentController(),
    private val menuBallController: MenuBallController = MenuBallController(),
    private val randomBounceAngleDegrees: () -> Float = {
        (Random.Default.nextFloat() * 2f - 1f) * MAX_RANDOM_BOUNCE_DEGREES
    }
) {

    private var phase = GamePhase.MENU
    private var selectedDifficulty = GameDifficulty.MEDIUM
    private var playerPaddlePosition = 0f
    private var opponentPaddlePosition = 0f
    private var ballX = CENTER
    private var ballY = SERVE_Y
    private var ballVelocityX = 0f
    private var ballVelocityY = 0f
    private var currentBallSpeed = selectedDifficulty.startingBallSpeed
    private var paddleHitCount = 0
    private var opponentScore = 0
    private var playerScore = 0
    private var scoreDisplayRemainingSeconds = 0f
    private var startAnimationElapsedSeconds = 0f

    fun resetMatch() {
        phase = GamePhase.MENU
        selectedDifficulty = GameDifficulty.MEDIUM
        opponentScore = 0
        playerScore = 0
        playerPaddlePosition = 0f
        opponentController.reset()
        opponentController.setDifficulty(selectedDifficulty)
        menuBallController.reset()
        opponentPaddlePosition = 0f
        currentBallSpeed = selectedDifficulty.startingBallSpeed
        paddleHitCount = 0
        ballX = CENTER
        ballY = SERVE_Y
        ballVelocityX = 0f
        ballVelocityY = 0f
        scoreDisplayRemainingSeconds = 0f
        startAnimationElapsedSeconds = 0f
    }

    fun update(elapsedSeconds: Float, playerPaddlePosition: Float) {
        this.playerPaddlePosition = playerPaddlePosition.coerceIn(-1f, 1f)
        var remaining = elapsedSeconds.coerceIn(0f, MAX_UPDATE_SECONDS)
        if (phase == GamePhase.MENU) {
            selectedDifficulty = when {
                this.playerPaddlePosition <= -MENU_SELECTION_THRESHOLD ->
                    GameDifficulty.EASY
                this.playerPaddlePosition >= MENU_SELECTION_THRESHOLD ->
                    GameDifficulty.HARD
                else -> GameDifficulty.MEDIUM
            }
            menuBallController.update(remaining)
        }

        opponentPaddlePosition = opponentController.update(
            elapsedSeconds = remaining,
            ballX = ballX,
            ballY = ballY,
            ballVelocityX = ballVelocityX,
            ballVelocityY = ballVelocityY,
            shouldTrackBall = phase == GamePhase.PLAYING && ballVelocityY < 0f
        )

        when (phase) {
            GamePhase.MENU -> return
            GamePhase.STARTING_ANIMATION -> {
                startAnimationElapsedSeconds += remaining
                if (startAnimationElapsedSeconds >= START_ANIMATION_SECONDS) {
                    startRound()
                }
                return
            }
            GamePhase.SHOWING_SCORE -> {
                scoreDisplayRemainingSeconds -= remaining
                if (scoreDisplayRemainingSeconds <= 0f) {
                    startRound()
                }
                return
            }
            GamePhase.MATCH_OVER -> return
            GamePhase.PLAYING -> Unit
        }

        while (remaining > 0f && phase == GamePhase.PLAYING) {
            val step = min(remaining, PHYSICS_STEP_SECONDS)
            ballX += ballVelocityX * step
            ballY += ballVelocityY * step
            handleBoundary()
            remaining -= step
        }
    }

    fun snapshot(): PongGameSnapshot {
        val menuBall = menuBallController.state()
        return PongGameSnapshot(
            phase = phase,
            playerPaddlePosition = playerPaddlePosition,
            ballX = ballX,
            ballY = ballY,
            opponentScore = opponentScore,
            playerScore = playerScore,
            opponentPaddlePosition = opponentPaddlePosition,
            selectedDifficulty = selectedDifficulty,
            startAnimationProgress = (
                startAnimationElapsedSeconds / START_ANIMATION_SECONDS
                ).coerceIn(0f, 1f),
            menuBallX = menuBall.x,
            menuBallY = menuBall.y
        )
    }

    fun confirmDifficultySelection(): Boolean {
        if (phase != GamePhase.MENU) {
            return false
        }

        opponentController.setDifficulty(selectedDifficulty)
        currentBallSpeed = selectedDifficulty.startingBallSpeed
        phase = GamePhase.STARTING_ANIMATION
        startAnimationElapsedSeconds = 0f
        ballVelocityX = 0f
        ballVelocityY = 0f
        return true
    }

    fun returnToDifficultyMenu(): Boolean {
        if (phase == GamePhase.MENU) {
            return false
        }

        resetMatch()
        return true
    }

    private fun handleBoundary() {
        val offsetX = ballX - CENTER
        val offsetY = ballY - CENTER
        val radius = hypot(offsetX, offsetY)
        if (radius < PADDLE_COLLISION_RADIUS) {
            return
        }

        val movingOutward = offsetX * ballVelocityX + offsetY * ballVelocityY > 0f
        if (!movingOutward) {
            return
        }

        val ballAngle = normalizeDegrees(
            atan2(offsetY, offsetX) * RADIANS_TO_DEGREES
        )
        val isBottomHalf = offsetY >= 0f
        val paddleCenterAngle = if (isBottomHalf) {
            PaddleFrameRenderer.playerCenterAngle(playerPaddlePosition)
        } else {
            PaddleFrameRenderer.enemyCenterAngle(opponentPaddlePosition)
        }
        val hitOffset = signedAngleDifference(ballAngle, paddleCenterAngle)
        val hitsPaddle = abs(hitOffset) <=
            PaddleFrameRenderer.PADDLE_HALF_ANGLE_DEGREES + BALL_ANGLE_ALLOWANCE

        if (hitsPaddle) {
            bounceFromPaddle(
                isBottomPaddle = isBottomHalf,
                hitOffset = hitOffset / (
                    PaddleFrameRenderer.PADDLE_HALF_ANGLE_DEGREES +
                        BALL_ANGLE_ALLOWANCE
                    ),
                offsetX = offsetX,
                offsetY = offsetY,
                radius = radius
            )
            moveBallInsideBoundary(offsetX, offsetY, radius)
            return
        }

        if (radius >= BALL_EXIT_RADIUS) {
            finishRound(playerScored = !isBottomHalf)
        }
    }

    private fun bounceFromPaddle(
        isBottomPaddle: Boolean,
        hitOffset: Float,
        offsetX: Float,
        offsetY: Float,
        radius: Float
    ) {
        paddleHitCount += 1
        if (paddleHitCount % selectedDifficulty.hitsPerSpeedIncrease == 0) {
            currentBallSpeed = (
                currentBallSpeed + selectedDifficulty.speedIncrease
                ).coerceAtMost(selectedDifficulty.maximumBallSpeed)
        }

        val normalX = offsetX / radius
        val normalY = offsetY / radius
        val velocityAlongNormal = ballVelocityX * normalX + ballVelocityY * normalY
        var outgoingX = ballVelocityX - 2f * velocityAlongNormal * normalX
        var outgoingY = ballVelocityY - 2f * velocityAlongNormal * normalY

        val clampedHitOffset = hitOffset.coerceIn(-1f, 1f)
        val endInfluence = clampedHitOffset.signPreservingSquare() * END_HIT_STRENGTH
        val tangentX = -normalY
        val tangentY = normalX
        outgoingX += tangentX * endInfluence * currentBallSpeed
        outgoingY += tangentY * endInfluence * currentBallSpeed

        val outgoingLength = hypot(outgoingX, outgoingY).coerceAtLeast(0.001f)
        outgoingX = outgoingX / outgoingLength * currentBallSpeed
        outgoingY = outgoingY / outgoingLength * currentBallSpeed

        val randomAngleRadians =
            randomBounceAngleDegrees().coerceIn(
                -MAX_RANDOM_BOUNCE_DEGREES,
                MAX_RANDOM_BOUNCE_DEGREES
            ) / RADIANS_TO_DEGREES
        val randomCos = cos(randomAngleRadians)
        val randomSin = sin(randomAngleRadians)
        val randomizedX = outgoingX * randomCos - outgoingY * randomSin
        val randomizedY = outgoingX * randomSin + outgoingY * randomCos

        setReachableVelocity(
            velocityX = randomizedX,
            velocityY = randomizedY,
            destinationCenterAngle = if (isBottomPaddle) {
                PaddleFrameRenderer.ENEMY_CENTER_ANGLE_DEGREES
            } else {
                PaddleFrameRenderer.PLAYER_CENTER_ANGLE_DEGREES
            }
        )
    }

    private fun setReachableVelocity(
        velocityX: Float,
        velocityY: Float,
        destinationCenterAngle: Float
    ) {
        val velocityLength = hypot(velocityX, velocityY).coerceAtLeast(0.001f)
        val directionX = velocityX / velocityLength
        val directionY = velocityY / velocityLength
        val offsetX = ballX - CENTER
        val offsetY = ballY - CENTER
        val positionAlongDirection = offsetX * directionX + offsetY * directionY
        val discriminant = positionAlongDirection * positionAlongDirection +
            PADDLE_COLLISION_RADIUS * PADDLE_COLLISION_RADIUS -
            offsetX * offsetX -
            offsetY * offsetY
        val intersectionDistance = -positionAlongDirection +
            sqrt(discriminant.coerceAtLeast(0f))
        val intersectionX = offsetX + directionX * intersectionDistance
        val intersectionY = offsetY + directionY * intersectionDistance
        val intersectionAngle = normalizeDegrees(
            atan2(intersectionY, intersectionX) * RADIANS_TO_DEGREES
        )
        val destinationOffset = signedAngleDifference(
            intersectionAngle,
            destinationCenterAngle
        ).coerceIn(-MAX_REACHABLE_ANGLE_DEGREES, MAX_REACHABLE_ANGLE_DEGREES)
        val targetAngleRadians =
            (destinationCenterAngle + destinationOffset) / RADIANS_TO_DEGREES
        val targetX = CENTER + PADDLE_COLLISION_RADIUS * cos(targetAngleRadians)
        val targetY = CENTER + PADDLE_COLLISION_RADIUS * sin(targetAngleRadians)

        val targetDirectionX = targetX - ballX
        val targetDirectionY = targetY - ballY
        val targetLength = hypot(targetDirectionX, targetDirectionY).coerceAtLeast(0.001f)
        ballVelocityX = targetDirectionX / targetLength * currentBallSpeed
        ballVelocityY = targetDirectionY / targetLength * currentBallSpeed
    }

    private fun Float.signPreservingSquare(): Float =
        if (this < 0f) {
            -abs(this).pow(END_HIT_EXPONENT)
        } else {
            this.pow(END_HIT_EXPONENT)
        }

    private fun moveBallInsideBoundary(
        offsetX: Float,
        offsetY: Float,
        radius: Float
    ) {
        val safeRadius = PADDLE_COLLISION_RADIUS - COLLISION_SEPARATION
        ballX = CENTER + offsetX / radius * safeRadius
        ballY = CENTER + offsetY / radius * safeRadius
    }

    private fun finishRound(playerScored: Boolean) {
        if (playerScored) {
            playerScore += 1
        } else {
            opponentScore += 1
        }
        if (playerScore >= WINNING_SCORE ||
            opponentScore >= WINNING_SCORE
        ) {
            phase = GamePhase.MATCH_OVER
            scoreDisplayRemainingSeconds = 0f
        } else {
            phase = GamePhase.SHOWING_SCORE
            scoreDisplayRemainingSeconds = SCORE_DISPLAY_SECONDS
        }
        ballVelocityX = 0f
        ballVelocityY = 0f
    }

    private fun startRound() {
        phase = GamePhase.PLAYING
        scoreDisplayRemainingSeconds = 0f
        startAnimationElapsedSeconds = START_ANIMATION_SECONDS
        ballX = CENTER
        ballY = SERVE_Y
        currentBallSpeed = selectedDifficulty.startingBallSpeed
        paddleHitCount = 0
        ballVelocityX = 0f
        ballVelocityY = currentBallSpeed
    }

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

    internal fun setBallForTest(
        x: Float,
        y: Float,
        velocityX: Float,
        velocityY: Float
    ) {
        phase = GamePhase.PLAYING
        ballX = x
        ballY = y
        ballVelocityX = velocityX
        ballVelocityY = velocityY
    }

    internal fun velocityForTest(): Pair<Float, Float> =
        ballVelocityX to ballVelocityY

    internal fun ballSpeedForTest(): Float = currentBallSpeed

    internal fun setScoresForTest(opponentScore: Int, playerScore: Int) {
        this.opponentScore = opponentScore
        this.playerScore = playerScore
    }

    internal fun startGameForTest(
        difficulty: GameDifficulty = GameDifficulty.EASY
    ) {
        selectedDifficulty = difficulty
        opponentController.setDifficulty(difficulty)
        currentBallSpeed = difficulty.startingBallSpeed
        startRound()
    }

    private companion object {
        private const val CENTER = 12f
        private const val SERVE_Y = CENTER - 4f
        private const val PADDLE_COLLISION_RADIUS = 11.2f
        private const val BALL_EXIT_RADIUS = 13.1f
        private const val COLLISION_SEPARATION = 0.02f
        private const val BALL_ANGLE_ALLOWANCE = 5f
        private const val END_HIT_STRENGTH = 1.15f
        private const val END_HIT_EXPONENT = 2f
        private const val MAX_RANDOM_BOUNCE_DEGREES = 4f
        private const val MAX_REACHABLE_ANGLE_DEGREES =
            PaddleFrameRenderer.MAX_TRAVEL_DEGREES +
                PaddleFrameRenderer.PADDLE_HALF_ANGLE_DEGREES +
                BALL_ANGLE_ALLOWANCE
        private const val SCORE_DISPLAY_SECONDS = 1.5f
        private const val WINNING_SCORE = 11
        private const val START_ANIMATION_SECONDS = 1f
        private const val MENU_SELECTION_THRESHOLD = 0.5f
        private const val PHYSICS_STEP_SECONDS = 0.008f
        private const val MAX_UPDATE_SECONDS = 0.05f
        private const val FULL_CIRCLE_DEGREES = 360f
        private const val HALF_CIRCLE_DEGREES = 180f
        private val RADIANS_TO_DEGREES = (180.0 / PI).toFloat()
    }
}
