package com.example.pongglyphtoy.glyph

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager

class PongGlyphToyService :
    GlyphToyService(LOG_TAG),
    SensorEventListener {

    private val frameHandler = Handler(Looper.getMainLooper())
    private val motionController = PaddleMotionController()
    private val gameEngine = PongGameEngine()
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var isRunning = false
    private var lastFrameTimeNanos = 0L

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) {
                return
            }

            val now = SystemClock.elapsedRealtimeNanos()
            val elapsedSeconds = if (lastFrameTimeNanos == 0L) {
                0f
            } else {
                ((now - lastFrameTimeNanos) / NANOS_PER_SECOND)
                    .toFloat()
                    .coerceAtMost(MAX_FRAME_DELTA_SECONDS)
            }
            lastFrameTimeNanos = now

            val paddlePosition = motionController.advanceFrame()
            gameEngine.update(elapsedSeconds, paddlePosition)
            renderGame()
            frameHandler.postDelayed(this, FRAME_INTERVAL_MS)
        }
    }

    override fun onGlyphMatrixReady(
        context: Context,
        manager: GlyphMatrixManager
    ) {
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        motionController.reset()
        gameEngine.resetMatch()
        lastFrameTimeNanos = SystemClock.elapsedRealtimeNanos()
        isRunning = true
        renderGame()

        val sensor = rotationSensor
        if (sensor == null) {
            Log.e(LOG_TAG, "No rotation-vector sensor is available")
        } else {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        frameHandler.post(frameRunnable)
    }

    override fun onGlyphButtonLongPressed() {
        if (gameEngine.confirmDifficultySelection()) {
            return
        }
        if (gameEngine.returnToDifficultyMenu()) {
            motionController.reset()
            renderGame()
            return
        }
        motionController.recalibrate()
    }

    override fun onGlyphMatrixDisconnected(context: Context) {
        isRunning = false
        frameHandler.removeCallbacks(frameRunnable)
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
        rotationSensor = null
        lastFrameTimeNanos = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_ROTATION_VECTOR
        ) {
            return
        }

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        motionController.updateRoll(orientation[ROLL_INDEX])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun renderGame() {
        glyphMatrixManager?.setMatrixFrame(PongFrameRenderer.render(gameEngine.snapshot()))
    }

    private companion object {
        private const val LOG_TAG = "PongGlyphToy"
        private const val ROLL_INDEX = 2
        private const val FRAME_INTERVAL_MS = 33L
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val MAX_FRAME_DELTA_SECONDS = 0.05f
    }
}
