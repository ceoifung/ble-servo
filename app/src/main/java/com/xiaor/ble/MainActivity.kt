package com.xiaor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.xiaor.libservo.BleStatus
import com.xiaor.libservo.BleWrapper
import com.xiaor.libservo.BoardMsg
import com.xiaor.libservo.KeyMsg
import com.xiaor.libservo.LightColor
import com.xiaor.libservo.MyBleManager
import com.xiaor.libservo.PowerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog:TextView

    private var curAngle = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLog)
        tvLog.movementMethod = ScrollingMovementMethod.getInstance()

        findViewById<Button>(R.id.btnConnectBle).setOnClickListener {
            MyBleManager.getDefault().disconnect()
            MyBleManager.getDefault().scanAndConnectBle(this)
        }
        findViewById<Button>(R.id.btnDisconnectBle).setOnClickListener {
            MyBleManager.getDefault().disconnect()
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            tvLog.text = ""
        }

        val tvHAngle = findViewById<TextView>(R.id.tvHAngle)
        val tvVAngle = findViewById<TextView>(R.id.tvVAngle)
        var isContinueAdd = false
        var isContinueMinus = false


        findViewById<SeekBar>(R.id.hSeekbar).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                BleWrapper.setHorizontalMoveAngle(p1)
//                appendLog("水平角度: $p1")
                tvHAngle.text = "$p1"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        findViewById<SeekBar>(R.id.vSeekbar).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                BleWrapper.setVerticalMoveAngle(p1)
//                appendLog("垂直角度: $p1")
                tvVAngle.text = "$p1"

            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        var isReverse = false
        var isReverse2 = false
        var findFace = false
        var startFindFace = false
        var count = 0
        var restoreAngle = 0
        val banCheckBox = findViewById<CheckBox>(R.id.checkboxBan)

        MyBleManager.getDefault().autoConnect = true
        MyBleManager.getDefault().requestPermissions(this)
        BleWrapper.registerMessageCallback(object :BleWrapper.IMessageCallbackListener{

            override fun onBoardStatusCallback(boardMsg: BoardMsg) {
                curAngle = boardMsg.horizontalAngle

                if (startFindFace){

                    if (count == 0){
                        if (curAngle <= 90){
                            appendLog("当前角度小于90，向0°转")
                            BleWrapper.setHorizontalMoveAngle(0)
                            count ++
                        }else{
                            appendLog("当前角度大于90，向180°转")
                            BleWrapper.setHorizontalMoveAngle(180)
                            count ++
                        }
                        return
                    }
                    if (findFace){
                        appendLog("找到人脸了")
                        BleWrapper.stopMove()
                        startFindFace = false
                        count = 0
                        isReverse = false
                        isReverse2 = false
                        return
                    }
                    appendLog("计数：${count}, 当前角度：${boardMsg.horizontalAngle}")
                    if (count > 2){
                        startFindFace = false
                        isReverse2 = false
                        isReverse = false
                        appendLog("已经找了两圈了，没有找到, 回复角度：${restoreAngle}")
                        BleWrapper.setHorizontalMoveAngle(restoreAngle)
                        return
                    }
                    if (curAngle >= 175){
                        if (isReverse){
                            return
                        }
                        isReverse = true
                        BleWrapper.setHorizontalMoveAngle(0)
                        count ++
                    }else if(curAngle <= 10){
                        if (isReverse2){
                            return
                        }
                        isReverse2 = true
                        BleWrapper.setHorizontalMoveAngle(180)
                        count ++
                    }
                }else{
                    if (!banCheckBox.isChecked){
                        if (!boardMsg.isHorizontalInCtl || !boardMsg.isVerticalInCtl) {
                            appendLog("遇到障碍物了，请下发stopMove接口")
                        }
                        appendLog("收到数据：$boardMsg")
                    }
                }
            }

            override fun onKeyStatusCallback(keyMsg: KeyMsg) {
                appendLog("收到按键数据：${keyMsg}")
            }

            override fun onPowerStatusCallback(powerStatus: PowerStatus) {
                appendLog("收到开关机数据：${powerStatus}")
            }

            override fun onRawDataCallback(data: ByteArray) {
                if (!startFindFace && !isContinueAdd && !isContinueMinus && !banCheckBox.isChecked){
                    appendLog("收到原始数据：${data.joinToString(separator = "") { byte ->
                        "%02x ".format(byte)
                    }}")
                }
            }

        })

        val singlePosSpinner = findViewById<Spinner>(R.id.singlePosSpinner)

        val singleModeSpinner = findViewById<Spinner>(R.id.singleModeSpinner)

        val allSpinner = findViewById<Spinner>(R.id.modeSpinner)

        findViewById<Button>(R.id.btnSendLight).setOnClickListener {
            BleWrapper.setSingleLight(singlePosSpinner.selectedItemPosition,
                LightColor.entries[singleModeSpinner.selectedItemPosition]
                )
        }

        findViewById<Button>(R.id.btnSendAll).setOnClickListener {
            BleWrapper.setLight(LightColor.entries[allSpinner.selectedItemPosition])
        }

        findViewById<Button>(R.id.btnPlusAngle).setOnClickListener {
            BleWrapper.setHorizontalMoveStep(3)
        }
        findViewById<Button>(R.id.btnMinusAngle).setOnClickListener {
            BleWrapper.setHorizontalMoveStep(-3)
        }
        findViewById<Button>(R.id.btnVPlusAngle).setOnClickListener {
            BleWrapper.setVerticalMoveStep(3)
        }
        findViewById<Button>(R.id.btnVMinusAngle).setOnClickListener {
            BleWrapper.setVerticalMoveStep(-3)
        }

        findViewById<Button>(R.id.btnStopMove).setOnClickListener {
            isContinueAdd = false
            isContinueMinus = false
            BleWrapper.stopMove()
        }
        findViewById<Button>(R.id.btnContinueAdd).setOnClickListener {
            isContinueAdd = true
            isContinueMinus = false
            Thread{
                while (isContinueAdd){
                    BleWrapper.setMotorMoveStep(3, 3)
                    Thread.sleep(50)
                }
            }.start()
        }
        findViewById<Button>(R.id.btnContinueMinus).setOnClickListener {
            isContinueMinus = true
            isContinueAdd = false
            Thread{
                while (isContinueMinus){
                    BleWrapper.setMotorMoveStep(-3, -3)
                    Thread.sleep(50)
                }
            }.start()
        }
        val btnReverse = findViewById<Button>(R.id.btnReverseAngle)
        btnReverse.setOnClickListener {
            appendLog("开始查找人脸，当前水平角度: $curAngle")
            startFindFace = true
            findFace = false
            isReverse2 = false
            isReverse = false
            count = 0
            restoreAngle = curAngle

        }

        findViewById<Button>(R.id.btnFindFace).setOnClickListener {
            appendLog("找到人脸，当前水平角度: $curAngle")
            findFace = true
        }

//        findViewById<CheckBox>(R.id.checkboxBan).setOnCheckedChangeListener { buttonView, isChecked ->
//            isBanReceived = isChecked
//        }

        findViewById<TextView>(R.id.tvAppVersion).text = getAppVersion(this)
    }

    /**
     * 获取应用版本信息
     *
     * 此函数尝试从给定的上下文中获取应用的版本号它首先获取应用的包名，
     * 然后通过包管理器获取包信息如果成功获取到包信息，则返回版本号；
     * 如果获取过程中发生NameNotFoundException异常，则表示未能找到包信息，
     * 此时返回一个表示失败的字符串
     *
     * @param context 上下文，用于访问应用的包管理器
     * @return 应用的版本信息字符串，如果获取失败则返回错误信息
     */
    private fun getAppVersion(context: Context): String {
        // 获取应用的包名
        val packageName = context.packageName
        // 尝试获取应用的版本信息
        return try {
            // 通过包名获取包信息
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            // 返回版本号
            "Ver ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            // 如果发生异常，打印异常信息并返回失败信息
            e.printStackTrace()
            "Failed to get version information"
        }
    }
    private fun appendLog(msg:String){
        tvLog.append(msg+"\n")
        val offset = getTextViewHeight()
        if (offset > tvLog.height) {
            tvLog.scrollTo(0, offset - tvLog.height)
        }
    }

    private fun getTextViewHeight(): Int {
        val layout = tvLog.layout
        val desired: Int = layout.getLineTop(tvLog.lineCount)
        val padding = tvLog.compoundPaddingTop + tvLog.compoundPaddingBottom
        return desired + padding
    }

    override fun onDestroy() {
        super.onDestroy()
        MyBleManager.getDefault().disconnect()
        MyBleManager.getDefault().unregisterBleReceiver(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MyBleManager.getDefault().onRequestPermissionsResult(this, requestCode, object :MyBleManager.BleStatusListener{
            override fun onStatusChanged(bleStatus: BleStatus) {
                appendLog("蓝牙链接状态：${bleStatus}")
                when (bleStatus) {
                    BleStatus.FAILURE -> {
                        appendLog("尝试重新连接蓝牙, reason: $bleStatus")
                        MyBleManager.getDefault().scanAndConnectBle(this@MainActivity)
                    }
                    BleStatus.NO_CALLBACK -> {
            //                    延时一秒重新连接
                        CoroutineScope(Dispatchers.IO).launch {
                            Log.e(TAG, "onStatusChanged: 延时一秒重新连接" )
                            appendLog("延时2秒重新连接")
                            delay(2000L)
                            MyBleManager.getDefault().disconnect()
                            MyBleManager.getDefault().scanAndConnectBle(this@MainActivity)
                        }

                    }
                    BleStatus.TOO_FREQUENTLY -> {
                        Log.e(TAG, "onStatusChanged: 数据发送过于频繁或者出错了", )
                    }
                    BleStatus.CONNECTED -> {
                        appendLog("已连接蓝牙: ${MyBleManager.getDefault().getConnectedBleDevice()}")
                    }

                    BleStatus.DISCONNECTED -> {
                        appendLog("蓝牙已断开连接")
                    }
                    else -> {

                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onBleDeviceFound(bleDevice: BluetoothDevice, rssi: Int, mac:String) {
                appendLog("扫描到匹配的蓝牙: ${bleDevice.name}, 信号强度：${rssi}, mac: $mac")
            }

            override fun onBleDeviceBonded(bondDeviceName: String) {
                appendLog("检测到蓝牙${bondDeviceName}已配对，正在解除绑定")
            }
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}