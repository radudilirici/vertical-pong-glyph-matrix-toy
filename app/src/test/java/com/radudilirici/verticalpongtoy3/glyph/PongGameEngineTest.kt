package com.radudilirici.verticalpongtoy3.glyph

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PongGameEngineTest {

    @Test
    fun ballInitiallyTravelsTowardPlayer() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        val initialY = engine.snapshot().ballY

        assertEquals(10f, initialY, 0.001f)
        engine.update(0.05f, playerPaddlePosition = 0f)

        assertTrue(engine.snapshot().ballY > initialY)
    }

    @Test
    fun centeredPlayerPaddleBouncesBallUpward() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setBallForTest(x = 12f, y = 22.9f, velocityX = 0f, velocityY = 7f)

        engine.update(0.05f, playerPaddlePosition = 0f)

        assertTrue(engine.velocityForTest().second < 0f)
    }

    @Test
    fun stationaryEnemyPaddleBouncesCenteredBallDownward() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setBallForTest(x = 12f, y = 1.1f, velocityX = 0f, velocityY = -7f)

        engine.update(0.05f, playerPaddlePosition = 0f)

        assertTrue(engine.velocityForTest().second > 0f)
    }

    @Test
    fun missingPlayerPaddleAwardsOpponentPoint() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setBallForTest(x = 12f, y = 24.9f, velocityX = 0f, velocityY = 7f)

        engine.update(0.05f, playerPaddlePosition = 1f)

        val snapshot = engine.snapshot()
        assertEquals(GamePhase.SHOWING_SCORE, snapshot.phase)
        assertEquals(1, snapshot.opponentScore)
        assertEquals(0, snapshot.playerScore)
    }

    @Test
    fun missingEnemyPaddleAwardsPlayerPoint() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setBallForTest(x = 5f, y = -0.9f, velocityX = 0f, velocityY = -7f)

        engine.update(0.05f, playerPaddlePosition = 0f)

        val snapshot = engine.snapshot()
        assertEquals(GamePhase.SHOWING_SCORE, snapshot.phase)
        assertEquals(0, snapshot.opponentScore)
        assertEquals(1, snapshot.playerScore)
    }

    @Test
    fun reachingTenShowsSetScoreBeforeNextRound() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setScoresForTest(opponentScore = 0, playerScore = 9)
        engine.setBallForTest(x = 5f, y = -0.9f, velocityX = 0f, velocityY = -7f)

        engine.update(0.05f, playerPaddlePosition = 0f)

        val snapshot = engine.snapshot()
        assertEquals(GamePhase.SHOWING_SCORE, snapshot.phase)
        assertEquals(10, snapshot.playerScore)
    }

    @Test
    fun playerWinsMatchAtElevenPoints() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setScoresForTest(opponentScore = 7, playerScore = 10)
        engine.setBallForTest(x = 5f, y = -0.9f, velocityX = 0f, velocityY = -7f)

        engine.update(0.05f, playerPaddlePosition = 0f)

        val snapshot = engine.snapshot()
        assertEquals(GamePhase.MATCH_OVER, snapshot.phase)
        assertEquals(11, snapshot.playerScore)
        assertEquals(7, snapshot.opponentScore)
    }

    @Test
    fun opponentWinsMatchAtElevenPoints() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setScoresForTest(opponentScore = 10, playerScore = 8)
        engine.setBallForTest(x = 12f, y = 24.9f, velocityX = 0f, velocityY = 7f)

        engine.update(0.05f, playerPaddlePosition = 1f)

        val snapshot = engine.snapshot()
        assertEquals(GamePhase.MATCH_OVER, snapshot.phase)
        assertEquals(11, snapshot.opponentScore)
        assertEquals(8, snapshot.playerScore)
    }

    @Test
    fun matchOverScreenRemainsUntilReturningToMenu() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setScoresForTest(opponentScore = 0, playerScore = 10)
        engine.setBallForTest(x = 5f, y = -0.9f, velocityX = 0f, velocityY = -7f)
        engine.update(0.05f, playerPaddlePosition = 0f)

        repeat(40) {
            engine.update(0.05f, playerPaddlePosition = 0f)
        }

        assertEquals(GamePhase.MATCH_OVER, engine.snapshot().phase)
        assertTrue(engine.returnToDifficultyMenu())
        assertEquals(GamePhase.MENU, engine.snapshot().phase)
    }

    @Test
    fun emptySideEdgeEndsRoundInsteadOfBouncing() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setBallForTest(x = 24.9f, y = 10f, velocityX = 7f, velocityY = 0f)

        engine.update(0.05f, playerPaddlePosition = 0f)

        val snapshot = engine.snapshot()
        assertEquals(GamePhase.SHOWING_SCORE, snapshot.phase)
        assertEquals(0, snapshot.opponentScore)
        assertEquals(1, snapshot.playerScore)
    }

    @Test
    fun offsetHitSteersBallAlongReachableTrajectory() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setBallForTest(x = 15f, y = 22.6f, velocityX = 0f, velocityY = 7f)

        engine.update(0.05f, playerPaddlePosition = 0f)

        val snapshot = engine.snapshot()
        val velocity = engine.velocityForTest()
        assertTrue(velocity.first > 0f)
        assertTrue(velocity.second < 0f)

        val destinationAngle = projectedBoundaryAngle(snapshot, velocity)
        val topOffset = abs(signedAngleDifference(destinationAngle, 270f))
        assertTrue(topOffset <= MAX_REACHABLE_ANGLE_DEGREES)
    }

    @Test
    fun paddleEndsDeflectMoreAggressivelyThanCenter() {
        val centerHit = PongGameEngine { 0f }
        centerHit.startGameForTest()
        centerHit.setBallForTest(x = 12f, y = 22.9f, velocityX = 0f, velocityY = 7f)
        centerHit.update(0.05f, playerPaddlePosition = 0f)

        val endHit = PongGameEngine { 0f }
        endHit.startGameForTest()
        endHit.setBallForTest(x = 15f, y = 22.6f, velocityX = 0f, velocityY = 7f)
        endHit.update(0.05f, playerPaddlePosition = 0f)

        assertTrue(
            abs(endHit.velocityForTest().first) >
                abs(centerHit.velocityForTest().first)
        )
    }

    @Test
    fun incomingDirectionInfluencesCurvedWallReflection() {
        val movingRight = PongGameEngine { 0f }
        movingRight.startGameForTest()
        movingRight.setBallForTest(x = 12f, y = 22.9f, velocityX = 2f, velocityY = 6.7f)
        movingRight.update(0.05f, playerPaddlePosition = 0f)

        val movingLeft = PongGameEngine { 0f }
        movingLeft.startGameForTest()
        movingLeft.setBallForTest(x = 12f, y = 22.9f, velocityX = -2f, velocityY = 6.7f)
        movingLeft.update(0.05f, playerPaddlePosition = 0f)

        assertNotEquals(
            movingRight.velocityForTest().first,
            movingLeft.velocityForTest().first,
            0.1f
        )
    }

    @Test
    fun randomBounceAngleBreaksRepeatedCenterTrajectory() {
        val clockwise = PongGameEngine { 4f }
        clockwise.startGameForTest()
        clockwise.setBallForTest(x = 12f, y = 22.9f, velocityX = 0f, velocityY = 7f)
        clockwise.update(0.05f, playerPaddlePosition = 0f)

        val counterClockwise = PongGameEngine { -4f }
        counterClockwise.startGameForTest()
        counterClockwise.setBallForTest(x = 12f, y = 22.9f, velocityX = 0f, velocityY = 7f)
        counterClockwise.update(0.05f, playerPaddlePosition = 0f)

        assertTrue(clockwise.velocityForTest().first > 0f)
        assertTrue(counterClockwise.velocityForTest().first < 0f)
    }

    @Test
    fun easyDifficultyIncreasesEverySecondHit() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest(GameDifficulty.EASY)
        val startingSpeed = GameDifficulty.EASY.startingBallSpeed

        bounceAtCenter(engine, hitNumber = 1)
        assertEquals(startingSpeed, engine.ballSpeedForTest(), 0.001f)

        bounceAtCenter(engine, hitNumber = 2)
        assertEquals(startingSpeed + 1f, engine.ballSpeedForTest(), 0.001f)
    }

    @Test
    fun ballSpeedUsesDifficultyMaximum() {
        val easy = PongGameEngine { 0f }
        easy.startGameForTest(GameDifficulty.EASY)
        repeat(30) { bounceAtCenter(easy, it + 1) }

        val medium = PongGameEngine { 0f }
        medium.startGameForTest(GameDifficulty.MEDIUM)
        repeat(30) { bounceAtCenter(medium, it + 1) }

        val hard = PongGameEngine { 0f }
        hard.startGameForTest(GameDifficulty.HARD)
        repeat(30) { bounceAtCenter(hard, it + 1) }

        assertEquals(GameDifficulty.EASY.maximumBallSpeed, easy.ballSpeedForTest(), 0.001f)
        assertEquals(GameDifficulty.MEDIUM.maximumBallSpeed, medium.ballSpeedForTest(), 0.001f)
        assertEquals(GameDifficulty.HARD.maximumBallSpeed, hard.ballSpeedForTest(), 0.001f)
    }

    @Test
    fun mediumIncreasesEverySecondHit() {
        val engine = PongGameEngine { 0f }

        engine.startGameForTest(GameDifficulty.MEDIUM)
        val startingSpeed = GameDifficulty.MEDIUM.startingBallSpeed

        assertEquals(startingSpeed, engine.ballSpeedForTest(), 0.001f)
        assertEquals(startingSpeed, hypot(
            engine.velocityForTest().first,
            engine.velocityForTest().second
        ), 0.001f)

        bounceAtCenter(engine, hitNumber = 1)

        assertEquals(startingSpeed, engine.ballSpeedForTest(), 0.001f)

        bounceAtCenter(engine, hitNumber = 2)

        assertEquals(startingSpeed + 1f, engine.ballSpeedForTest(), 0.001f)
    }

    @Test
    fun hardDifficultyStartsFasterAndIncreasesEveryHit() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest(GameDifficulty.HARD)
        val startingSpeed = GameDifficulty.HARD.startingBallSpeed

        assertEquals(startingSpeed, engine.ballSpeedForTest(), 0.001f)

        bounceAtCenter(engine, hitNumber = 1)

        assertEquals(startingSpeed + 1f, engine.ballSpeedForTest(), 0.001f)
    }

    @Test
    fun engineMovesOpponentTowardPredictedBallIntercept() {
        val engine = PongGameEngine(
            opponentController = OpponentController { 0.5f },
            randomBounceAngleDegrees = { 0f }
        )
        engine.startGameForTest()
        engine.setBallForTest(x = 12f, y = 12f, velocityX = 4f, velocityY = -6f)

        repeat(20) {
            engine.update(0.05f, playerPaddlePosition = 0f)
        }

        assertTrue(engine.snapshot().opponentPaddlePosition > 0f)
    }

    @Test
    fun scoreDisplayReturnsToNewRoundTowardPlayer() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest()
        engine.setBallForTest(x = 12f, y = -1f, velocityX = 0f, velocityY = -7f)
        engine.update(0.05f, playerPaddlePosition = 0f)

        repeat(40) {
            if (engine.snapshot().phase == GamePhase.PLAYING) {
                return@repeat
            }
            engine.update(0.05f, playerPaddlePosition = 0f)
        }

        val before = engine.snapshot()
        engine.update(0.01f, playerPaddlePosition = 0f)
        val after = engine.snapshot()

        assertEquals(GamePhase.PLAYING, after.phase)
        assertTrue(after.ballY > before.ballY)
    }

    @Test
    fun gameWaitsInMenuUntilDifficultyIsConfirmed() {
        val engine = PongGameEngine { 0f }
        engine.resetMatch()
        val initialBallY = engine.snapshot().ballY

        repeat(20) {
            engine.update(0.05f, playerPaddlePosition = 0f)
        }

        assertEquals(GamePhase.MENU, engine.snapshot().phase)
        assertEquals(GameDifficulty.MEDIUM, engine.snapshot().selectedDifficulty)
        assertEquals(initialBallY, engine.snapshot().ballY, 0.001f)
    }

    @Test
    fun menuBallMovesOnlyWhileMenuIsVisible() {
        val engine = PongGameEngine { 0f }
        engine.resetMatch()
        val initial = engine.snapshot()

        engine.update(0.05f, playerPaddlePosition = 0f)
        val moving = engine.snapshot()

        assertTrue(
            moving.menuBallX != initial.menuBallX ||
                moving.menuBallY != initial.menuBallY
        )

        engine.confirmDifficultySelection()
        engine.update(0.05f, playerPaddlePosition = 0f)
        val hidden = engine.snapshot()

        assertEquals(moving.menuBallX, hidden.menuBallX, 0.001f)
        assertEquals(moving.menuBallY, hidden.menuBallY, 0.001f)
    }

    @Test
    fun horizontalPaddlePositionSelectsAllDifficulties() {
        val engine = PongGameEngine { 0f }
        engine.resetMatch()

        engine.update(0.05f, playerPaddlePosition = -0.8f)
        assertEquals(GameDifficulty.EASY, engine.snapshot().selectedDifficulty)

        engine.update(0.05f, playerPaddlePosition = 0f)
        assertEquals(GameDifficulty.MEDIUM, engine.snapshot().selectedDifficulty)

        engine.update(0.05f, playerPaddlePosition = 0.8f)
        assertEquals(GameDifficulty.HARD, engine.snapshot().selectedDifficulty)
    }

    @Test
    fun longPressConfirmsHorizontallySelectedDifficulty() {
        val engine = PongGameEngine { 0f }
        engine.resetMatch()
        engine.update(0.05f, playerPaddlePosition = 0.8f)

        assertEquals(GameDifficulty.HARD, engine.snapshot().selectedDifficulty)
        assertTrue(engine.confirmDifficultySelection())
        assertEquals(GamePhase.STARTING_ANIMATION, engine.snapshot().phase)

        repeat(20) {
            engine.update(0.05f, playerPaddlePosition = 0f)
        }

        assertEquals(GamePhase.PLAYING, engine.snapshot().phase)
    }

    @Test
    fun inMatchLongPressReturnsToFreshMediumMenu() {
        val engine = PongGameEngine { 0f }
        engine.startGameForTest(GameDifficulty.HARD)
        engine.update(0.05f, playerPaddlePosition = 0.8f)

        assertTrue(engine.returnToDifficultyMenu())

        val snapshot = engine.snapshot()
        assertEquals(GamePhase.MENU, snapshot.phase)
        assertEquals(GameDifficulty.MEDIUM, snapshot.selectedDifficulty)
        assertEquals(0f, snapshot.playerPaddlePosition, 0.001f)
        assertEquals(0, snapshot.opponentScore)
        assertEquals(0, snapshot.playerScore)
        assertEquals(12f, snapshot.menuBallX, 0.001f)
        assertEquals(3f, snapshot.menuBallY, 0.001f)
    }

    private fun projectedBoundaryAngle(
        snapshot: PongGameSnapshot,
        velocity: Pair<Float, Float>
    ): Float {
        val directionLength = hypot(velocity.first, velocity.second)
        val directionX = velocity.first / directionLength
        val directionY = velocity.second / directionLength
        val offsetX = snapshot.ballX - CENTER
        val offsetY = snapshot.ballY - CENTER
        val positionAlongDirection = offsetX * directionX + offsetY * directionY
        val distance = -positionAlongDirection + sqrt(
            positionAlongDirection * positionAlongDirection +
                COLLISION_RADIUS * COLLISION_RADIUS -
                offsetX * offsetX -
                offsetY * offsetY
        )
        val intersectionX = offsetX + directionX * distance
        val intersectionY = offsetY + directionY * distance
        val degrees = atan2(intersectionY, intersectionX) * 180f / PI.toFloat()
        return if (degrees < 0f) degrees + 360f else degrees
    }

    private fun bounceAtCenter(engine: PongGameEngine, hitNumber: Int) {
        val isBottomHit = hitNumber % 2 == 1
        engine.setBallForTest(
            x = 12f,
            y = if (isBottomHit) 22.9f else 1.1f,
            velocityX = 0f,
            velocityY = if (isBottomHit) 7f else -7f
        )
        engine.update(0.05f, playerPaddlePosition = 0f)
    }

    private fun signedAngleDifference(angle: Float, reference: Float): Float {
        var difference = angle - reference
        if (difference > 180f) {
            difference -= 360f
        } else if (difference < -180f) {
            difference += 360f
        }
        return difference
    }

    private companion object {
        private const val CENTER = 12f
        private const val COLLISION_RADIUS = 11.2f
        private const val MAX_REACHABLE_ANGLE_DEGREES = 74f
    }
}
