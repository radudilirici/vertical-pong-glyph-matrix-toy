package com.radudilirici.verticalpongtoy3.glyph

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

abstract class GlyphToyService(private val logTag: String) : Service() {

    protected var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    private val eventHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            if (message.what != GlyphToy.MSG_GLYPH_TOY) {
                super.handleMessage(message)
                return
            }

            when (message.data?.getString(EVENT_DATA_KEY)) {
                GlyphToy.EVENT_ACTION_DOWN -> onGlyphButtonPressed()
                GlyphToy.EVENT_ACTION_UP -> onGlyphButtonReleased()
                GlyphToy.EVENT_CHANGE -> onGlyphButtonLongPressed()
                GlyphToy.EVENT_AOD -> onAodUpdate()
            }
        }
    }

    private val messenger = Messenger(eventHandler)

    private val managerCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
            Log.d(logTag, "Glyph Matrix service connected")
            glyphMatrixManager?.let { manager ->
                manager.register(Glyph.DEVICE_23112)
                onGlyphMatrixReady(applicationContext, manager)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Log.d(logTag, "Glyph Matrix service disconnected")
            onGlyphMatrixDisconnected(applicationContext)
        }
    }

    final override fun onBind(intent: Intent?): IBinder {
        Log.d(logTag, "Glyph Toy bound")

        val manager = GlyphMatrixManager.getInstance(applicationContext)
        if (manager == null) {
            Log.e(logTag, "Glyph Matrix service is unavailable")
        } else {
            glyphMatrixManager = manager
            manager.init(managerCallback)
        }
        return messenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        Log.d(logTag, "Glyph Toy unbound")
        onGlyphMatrixDisconnected(applicationContext)
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        return false
    }

    protected open fun onGlyphMatrixReady(
        context: Context,
        manager: GlyphMatrixManager
    ) = Unit

    protected open fun onGlyphMatrixDisconnected(context: Context) = Unit

    protected open fun onGlyphButtonPressed() = Unit

    protected open fun onGlyphButtonReleased() = Unit

    protected open fun onGlyphButtonLongPressed() = Unit

    protected open fun onAodUpdate() = Unit

    private companion object {
        private const val EVENT_DATA_KEY = "data"
    }
}
