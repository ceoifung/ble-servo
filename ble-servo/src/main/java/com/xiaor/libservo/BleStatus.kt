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
 *
 */
enum class BleStatus {
    CONNECTED, DISCONNECTED, CONNECTING, FAILURE, NOT_YET_CONNECTED, NOT_ENABLED,
    SEND_FAILURE
}