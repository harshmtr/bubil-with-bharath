package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ocr.TextRecognitionHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Product Safety Scanner", appName)
    }

    @Test
    fun `text recognition helper constant is configured`() {
        assertEquals(400L, TextRecognitionHelper.DEFAULT_THROTTLE_INTERVAL_MS)
    }
}
