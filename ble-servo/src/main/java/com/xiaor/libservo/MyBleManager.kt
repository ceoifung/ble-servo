package com.xiaor.libservo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleMtuChangedCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleRssiCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.callback.BleWriteCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.data.BleScanState
import com.clj.fastble.exception.BleException


class MyBleManager {

    private var permission = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private var myBleDevice: BleDevice? = null
    private val uuidService = "0000ffe0-0000-1000-8000-00805f9b34fb"
    private val uuidCharacteristicWrite = "0000ffe1-0000-1000-8000-00805f9b34fb"
    private val uuidCharacteristicNotify = "0000ffe1-0000-1000-8000-00805f9b34fb"
    private val bleName = "XiaoRGEEK"
    private var isRequestPermissions = false

    // 是否启动近场连接
    var enableRssi = true

    //    近场连接蓝牙的信号要求强度
    var nearRssi = -120
//    是否开启自动连接蓝牙设备
    var autoConnect = false


    private fun addPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!permission.contains(Manifest.permission.BLUETOOTH_CONNECT)) {
                permission.plus(Manifest.permission.BLUETOOTH_CONNECT)
                permission.plus(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
    }

    fun isBleEnable(): Boolean {
        return BleManager.getInstance().isBlueEnable
    }

    fun enableBle(context: Context) {
        addPermissions()
        if (BleManager.getInstance().isBlueEnable) return
        PermissionUtils.checkAndRequestMorePermissions(
            context,
            permissions = permission,
            XRConstant.REQUEST_CODE,
            object : PermissionUtils.PermissionRequestSuccessCallBack {
                override fun onHasPermission() {
                    if (PermissionUtils.checkPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    ) {
                        Log.e(TAG, "onHasPermission: has permission")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Log.e(TAG, "onHasPermission: enable ble")
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            (context as Activity).startActivityForResult(
                                intent,
                                XRConstant.BLE_OPEN_CODE
                            )
                        } else {
                            BleManager.getInstance().enableBluetooth()
                        }
                    } else {
                        Log.e(TAG, "onHasPermission: not has permission")
                        if (isRequestPermissions) {
                            return
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Log.d(TAG, "enableBle: request permissions")
                            PermissionUtils.requestMorePermissions(
                                context, arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                ), XRConstant.BLE_REQUEST_CODE
                            )
                            isRequestPermissions = true
                        }
                    }
                }
            })

    }

    fun disableBle(context: Context) {
        addPermissions()
        PermissionUtils.checkAndRequestMorePermissions(
            context,
            permissions = permission,
            XRConstant.REQUEST_CODE,
            object : PermissionUtils.PermissionRequestSuccessCallBack {
                override fun onHasPermission() {
                    BleManager.getInstance().disableBluetooth()
                }
            })
    }

    /**
     * 扫描并连接蓝牙，默认关闭自动连接
     * @param context 上下文
     */
    fun scanAndConnectBle(context: Context) {
        enableBle(context)
        if (PermissionUtils.checkMorePermissions(
                context,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            )
                .isNotEmpty()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionUtils.requestMorePermissions(
                    context, arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), XRConstant.BLE_SCAN_REQUEST
                )
                Log.e(TAG, "scanAndConnectBle: missing permissions")
                return
            }
        }
        disconnect()
        getBondedDevices()
        bleStatusListener?.onStatusChanged(BleStatus.SCANNING)
        var isStartScan = false
        var isOnScanning = false
        var startConnecting = false
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                Log.d(TAG, "onScanStarted: $success")
                isStartScan = success
            }

            override fun onScanning(bleDevice: BleDevice) {
                isOnScanning = true
                if (bleDevice.name != null) {

                    if (bleDevice.name.startsWith(bleName, true)) {
                        Log.w(
                            TAG,
                            "onScanning: found device: ${bleDevice.name} ${bleDevice.mac}, ${bleDevice.rssi}"
                        )
                        bleStatusListener?.onBleDeviceFound(
                            bleDevice.device,
                            bleDevice.rssi,
                            bleDevice.mac
                        )
                        if (autoConnect) {
                            if (enableRssi) {
                                Log.d(
                                    TAG,
                                    "onScanning: 当前蓝牙: ${bleDevice.name} 当前信号强度: ${bleDevice.rssi}, 设置的近场连接强度: $nearRssi"
                                )
                                if (bleDevice.rssi >= nearRssi) {
                                    startConnecting = true
                                    connectBle(bleDevice)
                                } else {
                                    Log.w(
                                        TAG,
                                        "onScanning: 当前蓝牙 ${bleDevice.name} 信号强度小于设置的近场蓝牙信号强度，不予连接\n" +
                                                "如需连接，可以减小nearRssi的值或者设置enableRssi=false"
                                    )
                                }
                            } else {
                                startConnecting = true
                                connectBle(bleDevice)
                            }
                        }
                    }
                }
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
//                优先NO_CALLBACK，如果检测到NO_CALLBACK，就不在往下执行了
                if (isStartScan && !isOnScanning) {
                    Log.e(TAG, "onScanFinished: could not find callback wrapper")
                    bleStatusListener?.onStatusChanged(BleStatus.NO_CALLBACK)
                    return
                }
//                如果没有自动连接，并且没有找到任何设备，就认为连接失败
                if (!startConnecting && autoConnect){
                    Log.d(TAG, "onScanFinished: 开启了自动连接，但是没有找到任何设备")
                    bleStatusListener?.onStatusChanged(BleStatus.FAILURE)
                    return
                }
                isStartScan = false
                isOnScanning = false
            }
        })
    }

    /**
     * 连接蓝牙设备
     */
    fun connectBle(bleDevice: BluetoothDevice) {
        val device = BleDevice(bleDevice)
        connectBle(device)
    }

    fun connectBle(bleDevice: BleDevice) {
        cancelScan()
        BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                Log.d(TAG, "onStartConnect: start connect ble")
                bleStatusListener?.onStatusChanged(BleStatus.CONNECTING)
            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                Log.e(TAG, "onConnectFail: ${bleDevice.name} connect fail")
                myBleDevice = null
                bleStatusListener?.onStatusChanged(BleStatus.FAILURE)
            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                Log.w(
                    TAG,
                    "onConnectSuccess: ${bleDevice.name} mac:${bleDevice.mac} rssi: ${bleDevice.rssi} connected"
                )
                myBleDevice = bleDevice
                cancelScan()
                startNotify()
                bleStatusListener?.onStatusChanged(BleStatus.CONNECTED)
            }

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                bleDevice: BleDevice,
                gatt: BluetoothGatt,
                status: Int
            ) {
                Log.e(TAG, "onDisConnected: ble disconnect")
                myBleDevice = null
                bleStatusListener?.onStatusChanged(BleStatus.DISCONNECTED)
            }
        })
    }

    fun cancelScan() {
        if (BleManager.getInstance().scanSate == BleScanState.STATE_SCANNING) {
            Log.d(TAG, "cancelScan: now stop scanning...")
            BleManager.getInstance().cancelScan()
        }
    }

    /**
     * 获取当前连接的蓝牙设备名称
     */
    fun getConnectedBleDevice(): String {
        return if (myBleDevice != null)
            myBleDevice!!.name
        else
            ""
    }

    /**
     * 写数据到蓝牙中去
     * @param context
     * @param data byte[] 数组
     */
    @Deprecated("do not need context", replaceWith = ReplaceWith("writeData(data)"))
    fun writeData(context: Context, data: ByteArray) {
        if (!isBleEnable()) {
            enableBle(context)
            return
        }
        if (myBleDevice == null) {
            scanAndConnectBle(context)
            return
        }
        BleManager.getInstance().write(
            myBleDevice,
            uuidService,
            uuidCharacteristicWrite,
            data,
            false,
            object : BleWriteCallback() {
                override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
                    Log.d(TAG, "onWriteSuccess: write success")
                }

                override fun onWriteFailure(exception: BleException) {
                    Log.e(TAG, "onWriteFailure: ${exception.description}")
                }
            })
    }

    @Deprecated(
        message = "do not need context",
        replaceWith = ReplaceWith("writeData(data.toByteArray())")
    )
    fun writeData(context: Context, data: String) {
        writeData(context, data.toByteArray())
    }

    /**
     * 发送数据
     * @param data bytes数组
     */
    fun writeData(data: ByteArray) {
        if (!isBleEnable()) {
            bleStatusListener?.onStatusChanged(BleStatus.NOT_ENABLED)
            throw Exception("bluetooth not enable")
        }
        if (myBleDevice == null) {
            bleStatusListener?.onStatusChanged(BleStatus.NOT_YET_CONNECTED)
            throw Exception("Not yet connect device")
        }
        Thread {
            BleManager.getInstance().write(
                myBleDevice,
                uuidService,
                uuidCharacteristicWrite,
                data,
                false,
                object : BleWriteCallback() {
                    override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
//                        Log.d(TAG, "onWriteSuccess: write success")
                    }

                    override fun onWriteFailure(exception: BleException) {
                        Log.e(
                            TAG,
                            "onWriteFailure: may be too frequently, slow it down, exception: ${exception.description}"
                        )
                        bleStatusListener?.onStatusChanged(BleStatus.TOO_FREQUENTLY)
                    }
                })
        }.start()
    }

    /**
     * 发送数据
     * @param data
     */
    fun writeData(data: String) {
        writeData(data.toByteArray())
    }

    private fun startNotify() {
        if (myBleDevice == null) {
            return
        }
        BleManager.getInstance().notify(
            myBleDevice,
            uuidService,
            uuidCharacteristicNotify,
            object : BleNotifyCallback() {
                override fun onNotifySuccess() {
                    Log.d(TAG, "onNotifySuccess: ")
                    setMtu(100)
                }

                override fun onNotifyFailure(exception: BleException) {
                    Log.e(TAG, "onNotifyFailure: ")
                }

                override fun onCharacteristicChanged(data: ByteArray) {
                    if (data.isNotEmpty()) {
                        try {
                            BleWrapper.requestDataDecode(data)
                        } catch (e: Exception) {
                            Log.e(TAG, "onCharacteristicChanged: 数据解析出错")
                        }
                    }
                }
            })
    }

    private fun stopNotify() {
        BleManager.getInstance().stopNotify(myBleDevice, uuidService, uuidCharacteristicNotify);
    }

    fun setMtu(mtu: Int) {
        if (myBleDevice == null) {
            Log.e(TAG, "setMtu: ble device cannot be null")
            return
        }
        BleManager.getInstance().setMtu(myBleDevice, mtu, object : BleMtuChangedCallback() {
            override fun onSetMTUFailure(exception: BleException) {
                Log.e(TAG, "onSetMTUFailure: ${exception.description}")
            }

            override fun onMtuChanged(mtu: Int) {
                Log.d(TAG, "onMtuChanged: $mtu")
            }
        })
    }

    fun disconnect() {
        cancelScan()
        stopNotify()
        if (myBleDevice != null) {
            BleManager.getInstance().disconnect(myBleDevice)
        }
    }

    fun getRssi() {
        if (myBleDevice == null) {
            return
        }
        BleManager.getInstance().readRssi(
            myBleDevice,
            object : BleRssiCallback() {
                override fun onRssiFailure(exception: BleException) {}
                override fun onRssiSuccess(rssi: Int) {}
            })
    }

    class InstanceManger {
        companion object {
            val instance = MyBleManager()
        }
    }

    companion object {
        private const val TAG = "MyBleManager"
        fun getDefault(): MyBleManager {
            return InstanceManger.instance
        }
    }

    private var bleReceiver: BleReceiver? = null
    private var bleStatusListener: BleStatusListener? = null


    /**
     * 全局注册
     * @param application
     */
    fun init(application: Application) {
        BleManager.getInstance().init(application)
        BleManager.getInstance().enableLog(false)
//        val scanRuleConfig = BleScanRuleConfig.Builder()
//            .setScanTimeOut(10000)
//            .build()
//        BleManager.getInstance().initScanRule(scanRuleConfig)
    }

    fun destroy() {
        BleManager.getInstance().destroy()
    }

    /**
     * 请求近场连接蓝牙权限
     */
    fun requestPermissions(context: Context) {
        val permission = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permission.plus(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        PermissionUtils.requestMorePermissions(context, permission, XRConstant.REQUEST_CODE)
    }

    /**
     * 连接蓝牙回调
     * @param context 上下文
     * @param requestCode 请求码
     * @param listener 注册蓝牙监听回调，蓝牙断开或者打开事件
     */
    fun onRequestPermissionsResult(
        context: Context,
        requestCode: Int,
        listener: BleStatusListener?
    ) {
        if (requestCode == XRConstant.BLE_REQUEST_CODE) {
            if (PermissionUtils.checkPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) &&
                PermissionUtils.checkPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            ) {
                Log.d(TAG, "onRequestPermissionsResult: open ble now scan ble")
                enableBle(context)
            }
        } else if (requestCode == XRConstant.REQUEST_CODE) {
            Log.d(TAG, "onRequestPermissionsResult: on request location")
            registerBleReceiver(context, listener)
        } else if (requestCode == XRConstant.BLE_SCAN_REQUEST) {
            Log.d(TAG, "onRequestPermissionsResult: scan ble")
            if (!isRequestPermissions) {
                scanAndConnectBle(context)
                isRequestPermissions = true
            } else {
                Log.e(TAG, "onRequestPermissionsResult: 重复申请")
            }
        }

    }


    /**
     * 注册监听
     * @param context 上下文
     * @param listener BleStatusListener
     */
    private fun registerBleReceiver(context: Context, listener: BleStatusListener?) {
        if (bleReceiver == null) {
            Log.d(TAG, "registerBleReceiver: register ble receiver")
            bleReceiver = BleReceiver()
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(bleReceiver, intentFilter)
            bleStatusListener = listener
        }
    }

    fun unregisterBleReceiver(context: Context) {
        if (bleReceiver != null) {
            try {
                context.unregisterReceiver(bleReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "unregisterBleReceiver: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(){
        if (BleManager.getInstance().isBlueEnable){
            val bondedDevices = BleManager.getInstance().bluetoothAdapter.bondedDevices
            for (device in bondedDevices){
                if (device.bondState == BluetoothDevice.BOND_BONDED){
                    if (!device.name.isNullOrEmpty()){
                        if (device.name.startsWith(bleName)){
                            Log.w(TAG, "getBondedDevices: find device ${device.name} bonded, now unpair device" )
                            bleStatusListener?.onBleDeviceBonded(device.name)
                            unpairDevice(device)
                        }
                    }
                }
            }
        }
    }

    /**
     * 解绑设备
     * @param device 蓝牙设备
     */
    private fun unpairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("removeBond")
            method.invoke(device)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface BleStatusListener {
        /**
         * 蓝牙连接状态回调
         */
        fun onStatusChanged(bleStatus: BleStatus)

        /**
         * 扫描到的能识别到的蓝牙设备
         */
        fun onBleDeviceFound(bleDevice: BluetoothDevice, rssi: Int, mac: String)

        /**
         * 符合条件的蓝牙设备的绑定回调
         */
        fun onBleDeviceBonded(bondDeviceName: String)
    }

    class BleReceiver : BroadcastReceiver() {
        override fun onReceive(p0: Context, p1: Intent?) {
            when (p1?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    Log.e(TAG, "onReceive: ACTION_STATE_CHANGED")
                    when (p1.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            Log.e(TAG, "onReceive: STATE_TURNING_ON")
                        }

                        BluetoothAdapter.STATE_ON -> {
                            Log.e(TAG, "onReceive: STATE_ON")
                            getDefault().scanAndConnectBle(p0)
                        }

                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            Log.e(TAG, "onReceive: STATE_TURNING_OFF")
                        }

                        BluetoothAdapter.STATE_OFF -> {
                            Log.e(TAG, "onReceive: STATE_OFF")
//                            如果人为关闭蓝牙，那么直接断开连接，并通知上层应用
                            getDefault().bleStatusListener?.onStatusChanged(BleStatus.DISCONNECTED)
                            getDefault().myBleDevice = null
                        }
                    }
                }
            }
        }
    }

}