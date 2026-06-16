package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.junit.Assert.assertNotNull

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import org.junit.Rule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()

  @Test
  fun `launch main activity`() {
    composeTestRule.onRoot().printToLog("ExampleRobolectricTest")
  }
}

