package com.radudilirici.verticalpongtoy3.glyph

import org.junit.Assert.assertTrue
import org.junit.Test

class MenuBallControllerTest {

    @Test
    fun ballReflectsFromVisibleEdge() {
        val controller = MenuBallController()
        controller.setStateForTest(
            x = 12f,
            y = 1.05f,
            velocityX = 0f,
            velocityY = -6f
        )

        controller.update(0.05f)

        assertTrue(controller.velocityForTest().second > 0f)
    }

    @Test
    fun ballReflectsFromInvisibleLineAboveText() {
        val controller = MenuBallController()
        controller.setStateForTest(
            x = 12f,
            y = 6.4f,
            velocityX = 0f,
            velocityY = 6f
        )

        controller.update(0.05f)

        assertTrue(controller.velocityForTest().second < 0f)
        assertTrue(controller.state().y <= 6.5f)
    }
}
