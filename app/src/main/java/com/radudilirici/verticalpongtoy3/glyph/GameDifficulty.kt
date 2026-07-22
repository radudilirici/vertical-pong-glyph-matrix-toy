package com.radudilirici.verticalpongtoy3.glyph

internal data class OpponentSettings(
    val minimumInitialReactionSeconds: Float,
    val maximumInitialReactionSeconds: Float,
    val minimumCorrectionIntervalSeconds: Float,
    val maximumCorrectionIntervalSeconds: Float,
    val maximumAimError: Float,
    val trackingGain: Float,
    val maximumPositionSpeed: Float,
    val maximumPositionAcceleration: Float
)

internal enum class GameDifficulty(
    val opponentSettings: OpponentSettings,
    val startingBallSpeed: Float,
    val maximumBallSpeed: Float,
    val hitsPerSpeedIncrease: Int,
    val speedIncrease: Float
) {
    EASY(
        opponentSettings = OpponentSettings(
            minimumInitialReactionSeconds = 0.3f,
            maximumInitialReactionSeconds = 0.6f,
            minimumCorrectionIntervalSeconds = 0.38f,
            maximumCorrectionIntervalSeconds = 0.62f,
            maximumAimError = 0.4f,
            trackingGain = 1.5f,
            maximumPositionSpeed = 1f,
            maximumPositionAcceleration = 1f
        ),
        startingBallSpeed = 10f,
        maximumBallSpeed = 15f,
        hitsPerSpeedIncrease = 2,
        speedIncrease = 1f
    ),
    MEDIUM(
        opponentSettings = OpponentSettings(
            minimumInitialReactionSeconds = 0.2f,
            maximumInitialReactionSeconds = 0.4f,
            minimumCorrectionIntervalSeconds = 0.2f,
            maximumCorrectionIntervalSeconds = 0.5f,
            maximumAimError = 0.3f,
            trackingGain = 2.5f,
            maximumPositionSpeed = 1.5f,
            maximumPositionAcceleration = 2f
        ),
        startingBallSpeed = 14f,
        maximumBallSpeed = 20f,
        hitsPerSpeedIncrease = 2,
        speedIncrease = 1f
    ),
    HARD(
        opponentSettings = OpponentSettings(
            minimumInitialReactionSeconds = 0.02f,
            maximumInitialReactionSeconds = 0.04f,
            minimumCorrectionIntervalSeconds = 0.05f,
            maximumCorrectionIntervalSeconds = 0.1f,
            maximumAimError = 0.15f,
            trackingGain = 5f,
            maximumPositionSpeed = 3f,
            maximumPositionAcceleration = 4f
        ),
        startingBallSpeed = 16f,
        maximumBallSpeed = 28f,
        hitsPerSpeedIncrease = 1,
        speedIncrease = 1f
    )
}
