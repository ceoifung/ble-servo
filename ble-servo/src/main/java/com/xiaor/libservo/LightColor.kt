package com.xiaor.libservo

/**
 * 灯光颜色通道值
 *
 *     OFF 全部关闭
 *     WHITE_GRAY 低亮度白色
 *     ORANGE 橙色
 *     YELLOW 黄色
 *     GREEN 绿色
 *     CYAN 青色
 *     BLUE 蓝色
 *     PURPLE 紫色
 *     WHITE 白色
 *     RED 红色
 *
 */
enum class LightColor(private val rgb: Byte) {
    OFF(0),
    WHITE_GRAY(1),
    ORANGE(2),
    YELLOW(3),
    GREEN(4),
    CYAN(5),
    BLUE(6),
    PURPLE(7),
    WHITE(8),
    RED(9);

    fun getColor() = rgb
}