package com.xiaor.ble

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.xiaor.libservo.BleStatus
import com.xiaor.libservo.MyBleManager
import com.xiaor.libservo.PermissionUtils
import com.xiaor.libservo.XRConstant
import com.xiaor.libservo.XRServoUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnConnectBle).setOnClickListener {
            MyBleManager.getDefault().scanAndConnectBle(this)

        }


        findViewById<SeekBar>(R.id.hSeekbar).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                MyBleManager.getDefault().setHorizontalMoveAngle(this@MainActivity, p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        findViewById<SeekBar>(R.id.vSeekbar).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                MyBleManager.getDefault().setVerticalMoveAngle(this@MainActivity, p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        MyBleManager.getDefault().requestPermissions(this)
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
        MyBleManager.getDefault().onRequestPermissionsResult(this, requestCode, null)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}