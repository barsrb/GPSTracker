package com.example.bars.gpstracker;

//import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServiceTracker extends Service {

    boolean trackerStop=false;
    //boolean lmStarted = false;
    DBHelper dbHelper;
    LocationManager LM;
    LocationListener ll = new GPSModule();

    Double lat ;
    Double lon ;
    String type ;

    public static String getName() {
        return "com.example.bars.gpstracker.ServiceTracker";
    }

    public void onCreate() {
        Toast toast = Toast.makeText(getApplicationContext(),"Запись включена!", Toast.LENGTH_SHORT);
        toast.show();
        LM = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        someTask();
        super.onCreate();
    }

    /*public int onStartCommand(Intent intent, int flags, int startId) {
        Toast toast = Toast.makeText(getApplicationContext(),"Запись включена 2!", Toast.LENGTH_SHORT);
        toast.show();

    }*/

    public void onDestroy() {
        trackerStop=true;
        dbHelper.close();
        LM.removeUpdates(ll);
        LM.removeUpdates(ll);
        LM.removeUpdates(ll);
        ll=null;
        //LM=null;
        //ll=null;
        Toast toast = Toast.makeText(getApplicationContext(),"Запись выключена!", Toast.LENGTH_SHORT);
        toast.show();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    void someTask() {

     dbHelper = new DBHelper(this);
     LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000*30, 50, ll);
     //LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000*10, 30, ll);


        new Thread(new Runnable() {
            public void run() {
                while (!trackerStop) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    String currentDateandTime = sdf.format(new Date());

                    if(lat!=null && lon!=null && !type.equals("0"))
                        saveData(currentDateandTime, lat.toString() ,lon.toString(), type);

                    type="0";

                    try {
                        Thread.sleep(30*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                stopSelf();
            }
        }).start();


    }

    void saveData(String date, String lat, String lon, String type)
    {

        // создаем объект для данных
        ContentValues cv = new ContentValues();

        // подключаемся к БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        cv.put("date", date);
        cv.put("lat", lat);
        cv.put("lon", lon);
        cv.put("type", type);
        // вставляем запись и получаем ее ID
        //long rowID =
        db.insert("points", null, cv);
        db.insert("points_storage", null, cv);
    }

    public class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "myDB", null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            // создаем таблицу с полями
            db.execSQL("create table points ("
                    + "id integer primary key autoincrement,"
                    + "date text,"
                    + "lat text,"
                    + "lon text,"
                    + "type text" + ");");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    class GPSModule implements LocationListener{

        @Override
        public void onLocationChanged(Location location) {
            if(location==null)
                type="0";
            else
            {
                if(location.getProvider().equals(LocationManager.GPS_PROVIDER))type="G";
                if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER))type="N";
                lat = location.getLatitude();
                lon = location.getLongitude();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }


}

