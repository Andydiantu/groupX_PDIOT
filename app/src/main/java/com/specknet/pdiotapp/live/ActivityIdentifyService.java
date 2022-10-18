package com.specknet.pdiotapp.live;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ActivityIdentifyService extends Service {
    private final String TAG = this.getClass().getName();

    public ActivityIdentifyService() {
    }

    public class ActivityIdentifyBinder extends Binder{
        public String cur_activity;
        public void setCurActivity(String cur_activity){ cur_activity = this.cur_activity; }
        public String getCurActivity() {
            return cur_activity;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "service onBind");
        return new ActivityIdentifyBinder();


    }
}