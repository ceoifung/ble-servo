package com.xiaor.libservo

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

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun test_protocol(){
        val data = Protocol.createMessage(2,3,2,4,5)

        data.forEach {
            print("${it.toHexString(HexFormat.UpperCase)},")
        }
        val data1 = byteArrayOf(2, 3, 2, 4, 5)
        val len = data1.size
        val crc = Protocol.calculateCRC(data1, len)
        println("CRC in Kotlin: ${crc.toInt()}")
    }

    @Test
    fun test_combine_data(){
        val byteValue: Byte = 0xF5.toByte()
        val intValue: Int = byteValue.toUByte().toInt()
        println(intValue) // 输出 245
    }
}