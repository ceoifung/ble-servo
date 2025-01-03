package com.xiaor.libservo

import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object BleWrapper {
    private var listener: IMessageCallbackListener? = null

    private const val TAG = "BleWrapper"

    private var maxLimitHorizontalAngle: Int = 180
    private var maxLimitVerticalAngle: Int = 180

    private fun combineHighAndLowBits(highBits: Byte, lowBits: Byte): Int {
        return (highBits.toUByte().toInt() shl 8) or lowBits.toUByte().toInt()
    }

    private fun decodeKeyStatus(status: Int): KeyStatus {
        return when (status) {
            0x00 -> KeyStatus.RELEASE
            0x01 -> KeyStatus.PRESSED
            0x02 -> KeyStatus.LONG_PRESSED
            else -> KeyStatus.ERROR
        }
    }

    private fun decodePowerStatus(status: Int): PowerStatus {
        return when (status) {
            0x00 -> PowerStatus.POWER_OFF
            0x01 -> PowerStatus.BOOT_UP
            else -> PowerStatus.ERROR
        }
    }

    /**
     * 解析回传数据，在蓝牙接收事件中调用
     * @param data
     */
    fun requestDataDecode(data: ByteArray) {
        if (data[0] == Protocol.header[0] && data[1] == Protocol.header[1]
            && data[data.size - 2] == Protocol.tail[1] && data[data.size - 1] == Protocol.tail[0]
        ) {
            if (data[2] == Protocol.TYPE_RECV) {
                val crcArray = data.sliceArray(2 until data.size - 3)
                val crc = Protocol.calculateCRC(crcArray, crcArray.size)
                if (crc == data[data.size - 3]) {
                    when (data[3]) {
                        Protocol.CTL_RECV -> {
                            if (data[4] >= 8.toByte()) {
                                val boardMsg = BoardMsg(
                                    combineHighAndLowBits(data[5], data[6]),
                                    combineHighAndLowBits(data[7], data[8]),
                                    (combineHighAndLowBits(
                                        data[9],
                                        data[10]
                                    ) / 1000.0).toFloat(),
                                    data[11] == 0x01.toByte(),
                                    data[12] == 0x01.toByte()
                                )
                                listener?.onBoardStatusCallback(boardMsg)
                            }

                        }

                        Protocol.CTL_KEY1_STATUS -> {
                            listener?.onKeyStatusCallback(
                                KeyMsg(
                                    1,
                                    decodeKeyStatus(data[5].toInt())
                                )
                            )
                        }

                        Protocol.CTL_KEY2_STATUS -> {
                            listener?.onKeyStatusCallback(
                                KeyMsg(
                                    2,
                                    decodeKeyStatus(data[5].toInt())
                                )
                            )
                        }

                        Protocol.CTL_POWER_STATUS -> {
                            listener?.onPowerStatusCallback(decodePowerStatus(data[5].toInt()))
                        }
                    }
                } else {
                    Log.e(TAG, "requestDataDecode: 数据crc校验出错")
                }
            }
        }
        listener?.onRawDataCallback(data)
    }

    /**
     * 发送数据，如果没有连接程序会抛异常
     * @param bytes 字节数组命令
     */
    private fun writeData(bytes: ByteArray) {
        try {
//            Log.e(TAG, "writeData：${bytes.joinToString(separator = "") { byte ->
//                "%02x ".format(byte)
//            }}" )
            MyBleManager.getDefault().writeData(bytes)
        } catch (ex: Exception) {
            Log.e(TAG, "writeData: ${ex.message}")
        }
    }

    /**
     * 设置最大的垂直电机的限位角度
     * @param angle 最大限制角度，默认是72度
     */
    fun setMaxLimitVerticalAngle(angle: Int) {
        maxLimitVerticalAngle = angle
    }

    /**
     * 设置最大的水平电机的限位角度
     * @param angle 最大限制角度，默认是180度
     */
    fun setMaxLimitHorizontalAngle(angle: Int) {
        maxLimitHorizontalAngle = angle
    }

    /**
     * 设置水平和云台电机的角度
     * @param horizontalAngle 水平电机角度
     * @param verticalAngle 垂直电机角度
     */
    fun setMotorMoveAngle(horizontalAngle: Int, verticalAngle: Int) {
        val currentX = max(0, min(maxLimitHorizontalAngle, horizontalAngle))
        val currentY = max(0, min(maxLimitVerticalAngle, verticalAngle))
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_SERVO, Protocol.CTL_SERVO, 4,
                ((currentX shr 8) and 0xFF).toByte(), (currentX and 0xff).toByte(),
                ((currentY shr 8) and 0xFF).toByte(), (currentY and 0xff).toByte()
            )
        )
    }

    /**
     * 以步幅的形式设置水平和云台电机的角度
     * @param horizontalStep 水平电机角度的步幅，最小为-20，最大为20
     * @param verticalStep 垂直电机角度的步幅，最小为-20，最大为20
     */
    fun setMotorMoveStep(horizontalStep: Int, verticalStep: Int) {
        val realHStep = max(-20, min(20, horizontalStep))
        val realVStep = max(-20, min(20, verticalStep))
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_SERVO, Protocol.CTL_ALL_STEP,
                4, if (realHStep < 0) 1 else 0, abs(realHStep).toByte(),
                if (realVStep < 0) 1 else 0, abs(realVStep).toByte()
            )
        )
    }

    /**
     * 设置水平云台舵机角度
     * @param angle
     */
    fun setHorizontalMoveAngle(angle: Int) {
        val currentX = max(0, min(maxLimitHorizontalAngle, angle))
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_SERVO, Protocol.CTL_H_SERVO, 2,
                ((currentX shr 8) and 0xFF).toByte(), (currentX and 0xff).toByte()
            )
        )
    }

    /**
     * 设置垂直云台舵机角度
     * @param angle
     */
    fun setVerticalMoveAngle(angle: Int) {
        val currentY = max(0, min(maxLimitVerticalAngle, angle))
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_SERVO, Protocol.CTL_V_SERVO, 2,
                ((currentY shr 8) and 0xFF).toByte(), (currentY and 0xff).toByte()
            )
        )
    }

    /**
     * 设置水平云台舵机步幅
     * @param step 步幅，最小为-20，最大为20
     */
    fun setHorizontalMoveStep(step: Int) {
//        Log.e(TAG, "setHorizontalMoveStep: $step" )
        val realStep = max(-20, min(20, step))
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_SERVO, Protocol.CTL_H_STEP,
                2, if (realStep < 0) 1 else 0, abs(realStep).toByte()
            )
        )
//        setMotorMoveStep(step, 0)
    }

    /**
     * 设置垂直云台舵机步幅
     * @param step 步幅，最小为-20，最大为20
     */
    fun setVerticalMoveStep(step: Int) {
        val realStep = max(-20, min(20, step))
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_SERVO, Protocol.CTL_V_STEP,
                2, if (realStep < 0) 1 else 0, abs(realStep).toByte()
            )
        )
//        setMotorMoveStep(0, step)
    }

    /**
     * 获取当前的角度，这个是上面设置的，底部的不一定是这个角度，用registerMessageCallback实时监听底部数据回传
     * @see registerMessageCallback
     */
    @Deprecated("Don't use it. please use registerMessageCallback", ReplaceWith("registerMessageCallback"))
    fun getCurrentHorizontalAngle(): Int {
        return -1
    }

    /**
     * 获取当前的角度，这个是上面设置的，底部的不一定是这个角度，用registerMessageCallback实时监听底部数据回传
     * @see registerMessageCallback
     */
    @Deprecated("Don't use it. please use registerMessageCallback", ReplaceWith("registerMessageCallback"))
    fun getCurrentVerticalAngle(): Int {
        return -1
    }

    /**
     * 设置全部彩灯灯光
     * @param color 相关的颜色
     * @see LightColor
     */
    fun setLight(color: LightColor) {
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_LIGHT,
                Protocol.CTL_LIGHT, 3, 2, 20, color.getColor()
            )
        )
    }

    /**
     * 设置单个彩灯的亮灭
     * @param position 位置，0代表电量灯
     * @param color 颜色，LightColor
     * @see LightColor 颜色通道枚举类型
     */
    fun setSingleLight(position: Int, color: LightColor) {
        val pos = max(0, min(20, position))
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_LIGHT,
                Protocol.CTL_LIGHT, 3, 4, pos.toByte(), color.getColor()
            )
        )
    }

    /**
     * 停止所有的电机转动
     *
     */
    fun stopMove() {
        stopMove(MotorDef.ALL)
    }

    /**
     * 停止水平或垂直的电机转动
     * @param motor 0是所有电机，1是水平云台舵机，2是垂直云台舵机
     * @see MotorDef 电机定义型号
     */
    fun stopMove(motor: MotorDef) {
        writeData(
            Protocol.createMessage(
                Protocol.TYPE_SERVO, Protocol.CTL_STOP,
                1, motor.getMotorId()
            )
        )
    }

    /**
     * 注册蓝牙消息监听器
     * @param callback IMessageCallbackListener
     * @see IMessageCallbackListener
     */
    fun registerMessageCallback(callback: IMessageCallbackListener) {
        listener = callback
    }

    interface IMessageCallbackListener {
        /**
         * 板子状态消息回调
         * @see BoardMsg
         */
        fun onBoardStatusCallback(boardMsg: BoardMsg)

        /**
         * 按键的的回调
         * @param keyMsg
         * @see KeyMsg
         */
        fun onKeyStatusCallback(keyMsg: KeyMsg)

        /**
         * 开关机按键的回调
         * @param powerStatus
         * @see PowerStatus
         */
        fun onPowerStatusCallback(powerStatus: PowerStatus)

        /**
         * 原始数据
         * @param data Byte数组
         */
        fun onRawDataCallback(data: ByteArray)
    }
}