package com.xiaor.libservo

/**
 * 板子的回传数据
 *
 *     horizontalAngle: 水平云台电机角度
 *     verticalAngle: 垂直云台电机角度
 *     batteryVoltage: 电池电压
 *
 *
 *
 */
data class BoardMsg(var horizontalAngle:Int, var verticalAngle:Int,var batteryVoltage:Int
    )

/**
 * 控制板按键状态事件
 *
 *      RELEASE 松手事件
 *      PRESSED 按下按键事件
 *
 */
enum class KeyStatus{
    RELEASE,
    PRESSED
}

/**
 * 控制板电源状态事件
 *
 *      BOOT_UP 开机事件
 *      POWER_OFF 关机时间
 *
 */
enum class PowerStatus{
    BOOT_UP,
    POWER_OFF
}

/**
 * 按键消息
 * @param id 获取按下的是哪一个按键
 * @param status 获取按键的状态
 */
data class KeyMsg(var id:Int, var status: KeyStatus)
