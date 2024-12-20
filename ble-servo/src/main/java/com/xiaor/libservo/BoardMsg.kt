package com.xiaor.libservo

/**
 * 板子的回传数据
 *
 *     horizontalAngle: 水平云台电机角度，单位度
 *     verticalAngle: 垂直云台电机角度，单位度
 *     batteryVoltage: 电池电压，单位毫伏
 *     isHorizontalInCtl: 水平云台电机受控状态和非受控状态，true为受控，false为非受控
 *     isVerticalInCtl: 垂直云台电机受控状态和非受控状态，true为受控，false为非受控
 *
 */
data class BoardMsg(var horizontalAngle:Int, var verticalAngle:Int,var batteryVoltage:Float,
    var isHorizontalInCtl: Boolean, var isVerticalInCtl: Boolean)

/**
 * 控制板按键状态事件
 *
 *      RELEASE 松手事件
 *      PRESSED 按下按键事件
 *      LONG_PRESSED 长按事件
 *      ERROR 错误数据
 *
 */
enum class KeyStatus{
    RELEASE,
    PRESSED,
    LONG_PRESSED,
    ERROR
}

/**
 * 控制板电源状态事件
 *
 *      BOOT_UP 开机事件
 *      POWER_OFF 关机时间
 *      ERROR 错误数据
 *
 */
enum class PowerStatus{
    BOOT_UP,
    POWER_OFF,
    ERROR
}

/**
 * 按键消息
 * @param id 获取按下的是哪一个按键
 * @param status 获取按键的状态
 * @see KeyStatus
 */
data class KeyMsg(var id:Int, var status: KeyStatus)
