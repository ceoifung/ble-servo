package com.xiaor.ble;

import android.app.Application;

import com.xiaor.libservo.MyBleManager;


public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MyBleManager.Companion.getDefault().init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        MyBleManager.Companion.getDefault().destroy();
    }
}
