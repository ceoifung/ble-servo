package com.xiaor.libservo

import android.util.Log
import kotlin.math.max
import kotlin.math.min

object BleWrapper {
    private var currentX = 90
    private var currentY = 90
    private var listener: IMessageCallbackListener?=null

    private const val TAG = "BleWrapper"

    private fun combineHighAndLowBits(highBits: Byte, lowBits: Byte): Int {
        return (highBits.toInt() shl 8) or lowBits.toInt()
    }

    /**
     * 解析回传数据，在蓝牙接收事件中调用
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
                            if (data[4] == 8.toByte()) {
                                val boardMsg = BoardMsg(
                                    combineHighAndLowBits(data[5], data[6]),
                                    combineHighAndLowBits(data[7], data[8]),
                                    combineHighAndLowBits(data[9], data[10]),
                                    data[11] == 0x01.toByte(),
                                    data[12] == 0x01.toByte()
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
                Log.e(TAG, "requestDataDecode: crc 校验失败" )

            }
        }
        listener?.onRawDataCallback(data)
    }

    /**
     * 设置角度
     */
    private fun setAngle(){
        MyBleManager.getDefault().writeData(Protocol.createMessage(Protocol.TYPE_SERVO, Protocol.CTL_SERVO, 4,
            ((currentX shr 8) and 0xFF).toByte(), (currentX and 0xff).toByte(),
            ((currentY shr 8) and 0xFF).toByte(), (currentY and 0xff).toByte()))
    }

    /**
     * 设置水平和云台电机的角度
     * @param horizontalAngle 水平电机角度
     * @param verticalAngle 垂直电机角度
     */
    fun setMotorMoveAngle(horizontalAngle: Int, verticalAngle: Int){
        currentX = max(0, min(180, horizontalAngle))
        currentY = max(0, min(180, verticalAngle))
        setAngle()
    }

    /**
     * 以步幅的形式设置水平和云台电机的角度
     * @param horizontalStep 水平电机角度的步幅
     * @param verticalStep 垂直电机角度的步幅
     */
    fun setMotorMoveStep(horizontalStep: Int, verticalStep: Int){
        currentX += horizontalStep
        currentX = max(0, min(180, currentX))
        currentY += verticalStep
        currentY = max(0, min(180, currentY))
        setAngle()
    }

    /**
     * 设置水平云台舵机角度
     * @param angle
     */
    fun setHorizontalMoveAngle(angle: Int) {
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentX = max(0, min(180, angle))
        setAngle()
    }

    /**
     * 设置垂直云台舵机角度
     * @param angle
     */
    fun setVerticalMoveAngle(angle: Int) {
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentY = max(0, min(180, angle))
        setAngle()
    }

    /**
     * 设置水平云台舵机步幅
     * @param step 步幅
     */
    fun setHorizontalMoveStep(step: Int){
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentX += step
        currentX = max(0, min(180, currentX))
        setAngle()
    }

    /**
     * 设置垂直云台舵机步幅
     * @param step 步幅
     */
    fun setVerticalMoveStep(step: Int) {
        //这个跟您原先的接口保持一致,但需要一个当前角度的返回值,
        //以便我们能够知道当前是否转到了最大值,后期我们需要这个最大值来进行停职云台动作
        currentY += step
        currentY = max(0, min(180, currentX))
        setAngle()
    }

    /**
     * 获取当前的角度，这个是上面设置的，底部的不一定是这个角度，用registerMessageCallback实时监听底部数据回传
     * @see registerMessageCallback
     */
    fun getCurrentHorizontalAngle(): Int {
        //获取当前的角度,方便复位,我们可以自己算出初始位置和现在角度的差距,
        //后期用我们算出的角度值,直接复位云台
        return currentX
    }

    /**
     * 获取当前的角度，这个是上面设置的，底部的不一定是这个角度，用registerMessageCallback实时监听底部数据回传
     * @see registerMessageCallback
     */
    fun getCurrentVerticalAngle(): Int {
        //获取当前的角度,方便复位,我们可以自己算出初始位置和现在角度的差距,
        //后期用我们算出的角度值,直接复位云台
        return currentY
    }

    /**
     * 设置全部彩灯灯光
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
     * @see IMessageCallbackListener
     */
    fun registerMessageCallback(callback: IMessageCallbackListener){
        listener = callback
    }

    interface IMessageCallbackListener{
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