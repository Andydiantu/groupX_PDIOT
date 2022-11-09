package com.specknet.pdiotapp.bluetooth;

import android.app.IntentService;
import android.content.Intent;
import android.app.Service;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


public class CloudService extends Service {

    private final String TAG = this.getClass().getName();
    private MyBinder myBinder;
    private boolean isBind = false;
    private int cnt = 0;

    //TODO
    public void updateDatabase() {
        cnt++;
    }

    public void run() {
        new Thread(){
            @Override
            public void run(){
                try{
                    while(isBind){
                        updateDatabase();
                        Log.i("CloudService", "service running"+cnt);
                        sleep(2000);
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }.start();
    }

    public class MyBinder extends Binder {
        CloudService getService() {
            return CloudService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "service onCreate");
        super.onCreate();
        myBinder = new MyBinder();
        isBind = true;
        run();
    }

    /**
     * Call when reconnect the service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service onStartCommand"+startId);
        isBind = true;
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Call when bind the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "service onBind");
        return myBinder;
    }

    /**
     * Call when rebind the service
     */
    @Override
    public void onRebind(Intent intent){
        Log.d(TAG, "service onRebind");
        super.onRebind(intent);
    }

    /**
     * Call when unbind the service
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "service onUnbind");
        return super.onUnbind(intent);
    }

    /**
     * Call when destroy the service
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "service onDestroy");
        isBind = false;
        stopSelf();
        super.onDestroy();
    }

}