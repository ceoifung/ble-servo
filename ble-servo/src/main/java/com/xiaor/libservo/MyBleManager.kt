package com.xiaor.libservo

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
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

//    近场连接蓝牙的信号要求强度
    var nearRssi = -90


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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Log.e(TAG, "enableBle: request permissions")
                            PermissionUtils.requestMorePermissions(
                                context, arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                ), XRConstant.BLE_REQUEST_CODE
                            )
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
        cancelScan()
        bleStatusListener?.onStatusChanged(BleStatus.CONNECTING)
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                Log.d(TAG, "onScanStarted: $success")
            }

            override fun onScanning(bleDevice: BleDevice) {
                if (bleDevice.name.startsWith(bleName, true)) {
                    if (bleDevice.rssi >= nearRssi) {
                        myBleDevice = bleDevice
                        connectBle()
                    }
                }
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
                if (myBleDevice == null) {
                    bleStatusListener?.onStatusChanged(BleStatus.FAILURE)
                }
            }
        })
    }

    fun connectBle() {
        BleManager.getInstance().connect(myBleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                Log.d(TAG, "onStartConnect: start connect ble")
                bleStatusListener?.onStatusChanged(BleStatus.CONNECTING)
            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                Log.e(TAG, "onConnectFail: ${bleDevice.name} connect fail")
                bleStatusListener?.onStatusChanged(BleStatus.FAILURE)
            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                Log.d(TAG, "onConnectSuccess: ${bleDevice.name} connected")
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
                bleStatusListener?.onStatusChanged(BleStatus.DISCONNECTED)
            }
        })
    }

    fun cancelScan() {
        if (BleManager.getInstance().scanSate == BleScanState.STATE_SCANNING) {
            BleManager.getInstance().cancelScan()
        }
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

    @Deprecated(message = "do not need context",
        replaceWith = ReplaceWith("writeData(data.toByteArray())")
    )
    fun writeData(context: Context, data: String) {
        writeData(context, data.toByteArray())
    }

    /**
     * 发送数据
     * @param data bytes数组
     */
    fun writeData(data: ByteArray){
//        Log.i(TAG, "writeData: data=${data.joinToString(separator = "") { byte ->
//            "%02x ".format(byte)
//        }}")
        if (!isBleEnable()) {
            Log.e(TAG, "writeData: bluetooth not enable" )
            return
        }
        if (myBleDevice == null) {
            Log.e(TAG,"writeData: Not yet scan ble device")
            return
        }
        Thread{
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
        }.start()
    }

    /**
     * 发送数据
     * @param data
     */
    fun writeData(data: String){
        writeData(data.toByteArray())
    }

    fun startNotify() {
        BleManager.getInstance().notify(
            myBleDevice,
            uuidService,
            uuidCharacteristicNotify,
            object : BleNotifyCallback() {
                override fun onNotifySuccess() {
                    Log.e(TAG, "onNotifySuccess: ")
                    setMtu(100)
                }

                override fun onNotifyFailure(exception: BleException) {
                    Log.e(TAG, "onNotifyFailure: ")
                }

                override fun onCharacteristicChanged(data: ByteArray) {
                    if (data.isNotEmpty()) {
                        Log.d(TAG, "onCharacteristicChanged: " + String(data))
                        BleWrapper.requestDataDecode(data)
                    }
                }
            })
    }

    private fun stopNotify() {
        BleManager.getInstance().stopNotify(myBleDevice, uuidService, uuidCharacteristicNotify);
    }

    fun setMtu(mtu: Int) {
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
        BleManager.getInstance().disconnect(myBleDevice)
    }

    fun getRssi() {
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
            scanAndConnectBle(context)
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

    interface BleStatusListener {
        fun onStatusChanged(bleStatus: BleStatus)
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
                        }
                    }
                }
            }
        }
    }

}