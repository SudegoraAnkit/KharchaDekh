package com.ankitsudegora

import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    @Test
    fun testMainActivityLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Activity has successfully launched without crashing
                assert(activity != null)
            }
        }
    }
}
