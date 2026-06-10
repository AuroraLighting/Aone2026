package com.aurora.aonev3

import org.json.JSONArray
import org.json.JSONException
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testFun() {
        var testString: String? = null
        val jsonArray = try {
            JSONArray(testString ?: "")
        } catch (ex: JSONException) {
            ex.printStackTrace()
            null
        }

        print(jsonArray)
        assert(true)
    }
}
