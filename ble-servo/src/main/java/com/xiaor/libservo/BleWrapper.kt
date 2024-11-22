package com.xiaor.libservo

import kotlin.math.max
import kotlin.math.min

object BleWrapper {
    private var currentX = 90
    private var currentY = 90
    private var listener: IMessageCallbackListener?=null

    private fun combineHighAndLowBits(highBits: Byte, lowBits: Byte): Int {
        return (highBits.toInt() shl 8) or lowBits.toInt()
    }

    /**
     * 解析回传数据
     * @param data
     */
    fun requestDataDecode(data: ByteArray){
        if (data[0] == Protocol.header[0] && data[1] == Protocol.header[1]
            && data[data.size - 2] == Protocol.tail[1] && data[data.size -1] == Protocol.tail[0]) {
            if (data[2] == Protocol.TYPE_RECV) {
                when (data[3]) {
                    Protocol.CTL_RECV -> {
                        val crcArray = data.sliceArray(2 until data.size - 4)
                        val crc = Protocol.calculateCRC(crcArray, crcArray.size)
                        if (crc == data[data.size - 3]) {
                            println("crc校验通过")
                            if (data[4] == 6.toByte()) {
                                val boardMsg = BoardMsg(
                                    combineHighAndLowBits(data[5], data[6]),
                                    combineHighAndLowBits(data[7], data[8]),
                                    combineHighAndLowBits(data[9], data[10]),
                                )
                                listener?.onBoardStatusCallback(boardMsg)
                            }
                        }
                    }
                    Protocol.CTL_KEY1_STATUS -> {
                        listener?.onKeyStatusCallback(KeyMsg(1, if (data[5] == 0x01.toByte()) KeyStatus.PRESSED else KeyStatus.RELEASE))
                    }
                    Protocol.CTL_KEY2_STATUS -> {
                        listener?.onKeyStatusCallback(KeyMsg(2, if (data[5] == 0x01.toByte()) KeyStatus.PRESSED else KeyStatus.RELEASE))
                    }

                    Protocol.CTL_POWER_STATUS -> {
                        listener?.onPowerStatusCallback(if (data[5] == 0x01.toByte()) PowerStatus.BOOT_UP else PowerStatus.POWER_OFF)
                    }
                }
            }else{
                    println("crc 校验失败")

            }
        }
        listener?.onRawDataCallback(data)
    }

    /**
     * 设置角度
     * @param value
     */
    private fun setAngle(value: Int){
        MyBleManager.getDefault().writeData(Protocol.createMessage(Protocol.TYPE_SERVO, Protocol.CTL_SERVO, 2,
            ((value shr 8) and 0xFF).toByte(), (value and 0xff).toByte()))
    }

    /**
     * 设置水平云台舵机角度
     * @param angle
     */
    fun setHorizontalMoveAngle(angle: Int): Int {
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentX = max(0, min(180, angle))
        setAngle(currentX)
        return currentX
    }

    /**
     * 设置垂直云台舵机角度
     * @param angle
     */
    fun setVerticalMoveAngle(angle: Int): Int {
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentY = max(0, min(180, angle))
        setAngle(currentY)
        return currentY
    }

    /**
     * 设置水平云台舵机步幅
     * @param step 步幅
     */
    fun setHorizontalMoveStep(step: Int): Int {
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentX += step
        currentX = max(0, min(180, currentX))
        setAngle(currentX)
        return currentX
    }

    /**
     * 设置垂直云台舵机步幅
     * @param step 步幅
     */
    fun setVerticalMoveStep(step: Int): Int {
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentY += step
        currentY = max(0, min(180, currentX))
        setAngle(currentY)
        return currentY
    }

    /**
     * 获取当前的角度，这个是上面设置的，底部的不一定是这个角度，用registerMessageCallback实时监听底部数据回传
     */
    fun getCurrentHorizontalAngle(): Int {
        //获取当前的角度,方便复位,我们可以自己算出初始位置和现在角度的差距,
        //后期用我们算出的角度值,直接复位云台
        return currentX
    }

    /**
     * 获取当前的角度，这个是上面设置的，底部的不一定是这个角度，用registerMessageCallback实时监听底部数据回传
     */
    fun getCurrentVerticalAngle(): Int {
        //获取当前的角度,方便复位,我们可以自己算出初始位置和现在角度的差距,
        //后期用我们算出的角度值,直接复位云台
        return currentY
    }

    /**
     * 设置灯光
     * @param color 相关的颜色
     * @see LightColor
     */
    fun setLight(color: LightColor) {
        MyBleManager.getDefault().writeData(Protocol.createMessage(Protocol.TYPE_LIGHT,
            Protocol.CTL_LIGHT,3,2,20, color.getColor()))
    }

    /**
     * 设置单个彩灯的亮灭
     * @param position 位置
     * @param color 颜色，LightColor
     * @see LightColor 颜色通道枚举类型
     */
    fun setSingleLight(position: Int, color: LightColor){
        val pos = max(1, min(20, position))
        MyBleManager.getDefault().writeData(Protocol.createMessage(Protocol.TYPE_LIGHT,
            Protocol.CTL_LIGHT,3,4,pos.toByte(), color.getColor()))
    }

    /**
     * 停止所有的电机转动
     *
     */
    fun stopMove(){
        stopMove(MotorDef.ALL)
    }

    /**
     * 停止水平或垂直的电机转动
     * @param motor 0是所有电机，1是水平云台舵机，2是垂直云台舵机
     * @see MotorDef 电机定义型号
     */
    fun stopMove(motor: MotorDef){
        MyBleManager.getDefault().writeData(Protocol.createMessage(Protocol.TYPE_SERVO, Protocol.CTL_STOP,
            1, motor.getMotorId()))
    }

    /**
     * 注册蓝牙消息监听器
     * @param callback IMessageCallbackListener
     */
    fun registerMessageCallback(callback: IMessageCallbackListener){
        listener = callback
    }

    interface IMessageCallbackListener{
        /**
         * 板子状态消息回调
         */
        fun onBoardStatusCallback(boardMsg: BoardMsg)

        /**
         * 按键的的回调
         * @param keyMsg
         */
        fun onKeyStatusCallback(keyMsg: KeyMsg)

        /**
         * 开关机按键的回调
         * @param powerStatus
         */
        fun onPowerStatusCallback(powerStatus: PowerStatus)

        /**
         * 原始数据
         * @param data Byte数组
         */
        fun onRawDataCallback(data: ByteArray)
    }
}