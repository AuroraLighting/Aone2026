package com.aurora.aonev3

import com.aurora.aonev3.logic.*
import org.json.JSONArray
import org.json.JSONException
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TriggerUnitTest {

    @Test
    fun testTriggersEqual() {
        val trigger1 = TimeOfDayTrigger(10, 10)
        val trigger2 = TimeOfDayTrigger(10, 10)
        assert(trigger1 == trigger2)
    }

    @Test
    fun testTriggersNotEqual() {
        val trigger1: Trigger = TimeOfDayTrigger(10, 10)
        val trigger2: Trigger = TimeOfDayTrigger("10", "10")
        val trigger3: Trigger = ResourceUpdateTrigger("10")
        assert(trigger1 != trigger2 &&
                trigger1 != trigger3 &&
                trigger2 != trigger3)
    }
}
