package com.aurora.aonev3

import com.aurora.aonev3.logic.Action
import com.aurora.aonev3.logic.GetResourceAction
import org.json.JSONArray
import org.json.JSONException
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ActionUnitTest {

    @Test
    fun testActionsEqual() {
        val json1 = this::class.java.classLoader?.getResource("actions.json")?.readText()
        val json1test = this::class.java.classLoader?.getResource("actions.json")?.readText()
        val actions1 = gson.fromJson(json1, Array<Action>::class.java)
        val actions1test = gson.fromJson(json1test, Array<Action>::class.java)

        assert(actions1.contentEquals(actions1test))
    }

    @Test
    fun testActionsNotEqual() {
        val json1 = this::class.java.classLoader?.getResource("actions.json")?.readText()
        val json2 = this::class.java.classLoader?.getResource("actions2.json")?.readText()
        val actions1 = gson.fromJson(json1, Array<Action>::class.java)
        val actions2 = gson.fromJson(json2, Array<Action>::class.java)

        assert(!actions1.contentEquals(actions2))
    }
}
