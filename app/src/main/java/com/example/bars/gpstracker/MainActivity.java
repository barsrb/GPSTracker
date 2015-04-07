package com.example.bars.gpstracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;


public class MainActivity extends Activity {

    ToggleButton btnService;
    boolean serviceStarted = false;
    Intent NSI; //NetworkServiceIntent
    public final static String BROADCAST_ACTION = "com.example.bars.gpstracker.servicebroadcast";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnService = (ToggleButton) findViewById(R.id.btnService);
        checkServiceStatus();
        //startService(new Intent(this, NetworkService.class));
    }

    protected void onStart() {
        super.onStart();
        checkServiceStatus();
    }

    protected void onResume() {
        super.onResume();
        checkServiceStatus();
    }

    protected void onRestart() {
        super.onRestart();
        checkServiceStatus();
    }

    public void btn_Service_onclick(View view){
        if(btnService.isChecked()){
            serviceStarted=true;
            startService(new Intent(this, ServiceTracker.class));
            NSI = new Intent(this, NetworkService.class);
            startService(NSI);
        }
        else {
            serviceStarted=false;
            stopService(new Intent(this, ServiceTracker.class));
            NSI = new Intent(NetworkService.BROADCAST_ACTION).putExtra("stop", 1);
            sendBroadcast(NSI);
        }
    }

    public void checkServiceStatus(){

        if (serviceStarted){
            btnService.setChecked(true);
        }
        else {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(100)) {
                if (ServiceTracker.getName().equals(service.service.getClassName())) {
                    btnService.setChecked(true);
                    serviceStarted = true;
                    break;
                } else {
                    btnService.setChecked(false);
                    serviceStarted = false;
                }
            }
        }
    }

}
