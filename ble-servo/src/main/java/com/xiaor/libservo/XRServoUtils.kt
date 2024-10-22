package com.xiaor.libservo


class XRServoUtils {

    private var currentX = 90
    private var currentY = 90

    /**
     * 组包通信协议
     * @param b1 控制位
     * @param b2 舵机ID
     * @param b3 舵机角度
     */
    private fun createCmdBuffer(b1: Byte, b2: Byte, b3: Int): ByteArray {
        val cmd = ByteArray(5)
        cmd[0] = 0xff.toByte()
        cmd[1] = b1
        cmd[2] = b2
        cmd[3] = b3.toByte()
        cmd[4] = 0xff.toByte()
        return cmd
    }

    /**
     * 获取上一次设置的水平云台角度
     * @return 垂直云台角度
     */
    fun getLastHorizontalAngle(): Int {
        return currentX
    }

    /**
     * 获取上一次设置的垂直云台角度
     * @return 垂直云台角度
     */
    fun getLastVerticalAngle(): Int {
        return currentY
    }

    /**
     * 设置水平云台舵机转动到角度
     * @param angle 角度，范围是0-180
     */
    fun createHorizontalMoveAngle(angle: Int): ByteArray {
        currentX = angle
        if (currentX > 180) currentX = 180
        if (currentX < 0) currentX = 0
        return createCmdBuffer(1, 7, currentX)
    }

    /**
     * 设置垂直云台舵机转动到角度
     * @param angle 角度，范围是0-180
     */
    fun createVerticalMoveAngle(angle: Int): ByteArray  {
        currentY = angle
        if (currentY > 180) currentY = 180
        if (currentY < 0) currentY = 0
        return createCmdBuffer(1, 8, currentY)
    }

    /**
     * 设置水平云台舵机在当前角度上在加几度
     * @param step 在当前的角度下左转或者右转多少度
     */
    fun createHorizontalMoveStep(step: Int): ByteArray  {
        currentX += step
        if (currentX > 180) currentX = 180
        if (currentX < 0) currentX = 0
        return createCmdBuffer(1, 7, currentX)
    }

    /**
     * 设置垂直云台舵机在当前角度上在加几度
     * @param step 在当前的角度下往上或者往下多少度
     */
    fun createVerticalMoveStep(step: Int): ByteArray  {
        currentY += step
        if (currentY > 180) currentY = 180
        if (currentY < 0) currentY = 0
        return createCmdBuffer(1, 8, currentY)
    }
}