package com.xiaor.libservo

/**
 * 蓝牙状态
 *
 *      CONNECTED 已连接或链接成功
 *      DISCONNECTED 已断连
 *      CONNECTING 正在连接
 *      FAILURE 连接失败
 *      NOT_YET_CONNECTED 尚未连接
 *      NOT_ENABLED 蓝牙未打开
 *      SEND_FAILURE 发送数据失败
 *      TOO_FREQUENTLY 发送数据太频繁了
 *      NO_CALLBACK 蓝牙扫描太过频繁，导致系统没有回调，检测到这个标志位之后，调用disconnect, 然后再重新扫描连接或者延时一下
 *
 */
enum class BleStatus {
    CONNECTED, DISCONNECTED, CONNECTING, FAILURE, NOT_YET_CONNECTED, NOT_ENABLED,
    SEND_FAILURE, TOO_FREQUENTLY, NO_CALLBACK
}