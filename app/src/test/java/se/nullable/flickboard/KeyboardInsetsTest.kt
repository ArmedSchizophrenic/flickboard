package se.nullable.flickboard

import android.app.Activity
import android.os.Looper
import android.view.View
import android.inputmethodservice.InputMethodService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardInsetsTest {
    @Test fun resizingKeyboardRequestsNewInsetsEvenWhenFullScreenViewDoesNotResize() {
        val controller = Robolectric.buildService(KeyboardService::class.java).create()
        val service = controller.get()
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val view = object : View(activity.get()) {
            var layoutRequests = 0
            override fun requestLayout() { layoutRequests++ }
        }
        activity.get().setContentView(view)
        service.updateInputGeometry(view, viewHeight = 2000, keyboardHeight = 600)
        shadowOf(Looper.getMainLooper()).idle()
        val requestsBeforeResize = view.layoutRequests
        service.updateInputGeometry(view, keyboardHeight = 900)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(view.layoutRequests > requestsBeforeResize)
        val insets = InputMethodService.Insets()
        service.onComputeInsets(insets)
        assertEquals(1100, insets.contentTopInsets)
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_CONTENT, insets.touchableInsets)
        service.updateInputGeometry(view, keyboardHeight = 400)
        shadowOf(Looper.getMainLooper()).idle()
        service.onComputeInsets(insets)
        assertEquals(1600, insets.contentTopInsets)
        val requestsAfterResize = view.layoutRequests
        service.updateInputGeometry(view, keyboardHeight = 400)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(requestsAfterResize, view.layoutRequests)
        controller.destroy()
        activity.pause().stop().destroy()
    }
}
