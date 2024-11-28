package com.xiaor.ble

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
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

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLog)
        tvLog.movementMethod = ScrollingMovementMethod.getInstance()

        findViewById<Button>(R.id.btnConnectBle).setOnClickListener {
            MyBleManager.getDefault().scanAndConnectBle(this)

        }


        findViewById<SeekBar>(R.id.hSeekbar).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                BleWrapper.setHorizontalMoveAngle(p1)
                appendLog("水平角度: $p1")
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
                appendLog("垂直角度: $p1")
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        MyBleManager.getDefault().requestPermissions(this)
        BleWrapper.registerMessageCallback(object :BleWrapper.IMessageCallbackListener{

            override fun onBoardStatusCallback(boardMsg: BoardMsg) {
                appendLog("收到解析数据：${boardMsg}")
            }

            override fun onKeyStatusCallback(keyMsg: KeyMsg) {
                appendLog("收到按键数据：${keyMsg}")
            }

            override fun onPowerStatusCallback(powerStatus: PowerStatus) {
                appendLog("收到开关机数据：${powerStatus}")
            }

            override fun onRawDataCallback(data: ByteArray) {
                appendLog("收到原始数据：${data.joinToString(separator = "") { byte ->
                    "%02x ".format(byte)
                }}")
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
            }
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}