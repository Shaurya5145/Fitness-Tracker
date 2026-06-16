package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `application package name is correct`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("com.aistudio.fitnesstracker.zyxwvu", context.packageName)
  }
}

