package com.xiaor.ble

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
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
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog:TextView

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
        var isConntinueAdd = false
        var isConntinueMinus = false


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

        MyBleManager.getDefault().requestPermissions(this)
        BleWrapper.registerMessageCallback(object :BleWrapper.IMessageCallbackListener{

            override fun onBoardStatusCallback(boardMsg: BoardMsg) {
                if (isReverse){
                    appendLog("hAngle: ${boardMsg.horizontalAngle}, isH: ${boardMsg.isHorizontalInCtl}, vAngle: ${boardMsg.verticalAngle}, isV: ${boardMsg.isVerticalInCtl}")
                    if (boardMsg.horizontalAngle <=10){
                        BleWrapper.setHorizontalMoveAngle(180)
                    }else if(boardMsg.horizontalAngle >= 180){
                        BleWrapper.setHorizontalMoveAngle(0)
                    }
                }else{
                    if (!isConntinueAdd && !isConntinueMinus){
                        appendLog("收到电源板数据：${boardMsg}")
                    }else{
                        appendLog("hAngle: ${boardMsg.horizontalAngle}, isH: ${boardMsg.isHorizontalInCtl}, vAngle: ${boardMsg.verticalAngle}, isV: ${boardMsg.isVerticalInCtl}")
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
                if (!isReverse && !isConntinueAdd && !isConntinueMinus){
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
            isConntinueAdd = false
            isConntinueMinus = false
            BleWrapper.stopMove()
        }
        findViewById<Button>(R.id.btnContinueAdd).setOnClickListener {
            isConntinueAdd = true
            isConntinueMinus = false
            Thread{
                while (isConntinueAdd){
                    BleWrapper.setMotorMoveStep(3, 3)
                    Thread.sleep(50)
                }
            }.start()
        }
        findViewById<Button>(R.id.btnContinueMinus).setOnClickListener {
            isConntinueMinus = true
            isConntinueAdd = false
            Thread{
                while (isConntinueMinus){
                    BleWrapper.setMotorMoveStep(-3, -3)
                    Thread.sleep(50)
                }
            }.start()
        }
        val btnReverse = findViewById<Button>(R.id.btnReverseAngle)
        btnReverse.setOnClickListener {
            isReverse = !isReverse
            if (isReverse){
                BleWrapper.setHorizontalMoveAngle(180)
                btnReverse.text = "停止"
            }else{
                btnReverse.text = "循环"
                BleWrapper.stopMove()
            }
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
            }
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}