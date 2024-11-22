package com.xiaor.libservo

/**
 * 电机的定义
 * @param motorId 电机的ID
 *
 *      ALL 所有电机
 *      HORIZONTAL 水平电机
 *      VERTICAL 垂直电机
 */
enum class MotorDef(private val motorId: Byte){
    ALL(0),
    HORIZONTAL(1),
    VERTICAL(2);
    fun getMotorId()  = motorId
}