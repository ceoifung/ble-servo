package com.xiaor.libservo

import kotlin.experimental.xor

object Protocol {

    val header = listOf<Byte>(0xaa.toByte(), 0x55)
    val tail = listOf<Byte>(0xee.toByte(), 0xee.toByte())
//  舵机的控制逻辑
    val TYPE_SERVO:Byte = 0x01
    val CTL_SERVO:Byte  = 0x00
    val CTL_STOP:Byte = 0x01
//  彩灯的控制
    val TYPE_LIGHT:Byte = 0x02
    val CTL_LIGHT:Byte = 0x03
    val TYPE_RECV:Byte = 0x80.toByte()
    val CTL_RECV:Byte = 0x01
    val CTL_KEY1_STATUS:Byte = 0x02
    val CTL_KEY2_STATUS:Byte = 0x03
    val CTL_POWER_STATUS:Byte = 0x04

    val CTL_H_STEP:Byte = 0x02
    val CTL_V_STEP:Byte = 0x03
    val CTL_ALL_STEP:Byte = 0x04

    /**
     * 创建消息
     * @param data 不定长的byte数组
     * @return cmd 返回组包后的命令
     */
    fun createMessage(vararg data: Byte): ByteArray{
        val length = data.size + header.size + tail.size + 1
        val cmd = ByteArray(length)
        // 将 header 添加到 cmd 数组中
        for (i in header.indices) {
            cmd[i] = header[i]
        }
        // 将 data 添加到 cmd 数组中
        for (i in header.size until header.size + data.size) {
            cmd[i] = data[i - header.size]
        }

        val crc = calculateCRC(data, data.size)
        cmd[header.size + data.size] = crc
        // 将 tail 添加到 cmd 数组中
        for (i in (header.size + data.size+1) until length) {
            cmd[i] = tail[i - header.size - data.size - 1]
        }
        return cmd
    }

    /**
     * crc校验
     * @param data 数据
     * @param len 数据长度
     * @return crc校验值
     */
    fun calculateCRC(data: ByteArray, len: Int): Byte {
        var crc = 0.toByte()
        for (i in 0 until len) {
            crc = crc xor data[i]
            for (j in 0..7) {
                crc = if (crc.toUByte().toInt() and 0x80 == 0x80) {
                    (crc.toUByte().toInt() shl 1).toByte() xor 0x07.toByte() // CRC-8 algorithm polynomial is 0x07
                } else {
                    (crc.toUByte().toInt() shl 1).toByte()
                }
            }
        }
        return crc
    }

}