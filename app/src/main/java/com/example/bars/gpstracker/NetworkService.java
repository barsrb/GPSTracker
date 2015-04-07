package com.example.bars.gpstracker;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class NetworkService extends Service{
    public NetworkService() {
    }
    HttpClient client;
    HttpPost post;
    HttpResponse response;
    String responseStr;
    List<NameValuePair> pairs;
    boolean NetworkServiceStop = false;
    BroadcastReceiver br;
    public final static String BROADCAST_ACTION = "com.example.bars.gpstracker.servicebroadcast";

    boolean TracerServiceStop = true;
    DBHelper dbHelper;
    int row_id;
    String lat, lon, date, type;
    String deviceID;

    @Override
    public void onCreate() {
        String address = "http://aromata.ru/gps/save_data.php";
        client = new DefaultHttpClient();
        post = new HttpPost(address);
        NetworkServiceStop=false;
        TracerServiceStop = false;
        serviceTask();
        TelephonyManager mngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        deviceID = mngr.getDeviceId();
        if(deviceID==null)deviceID="000000000000000";
        super.onCreate();
        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                int stop = intent.getIntExtra("stop", 0);

                // Ловим сообщения о старте задач
                if (stop  == 1) {
                    TracerServiceStop = true;
                }

                }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(br, intFilt);
    }

    @Override
    public void onDestroy() {
        //NetworkServiceStop=true;
        TracerServiceStop = true;
        super.onDestroy();
    }

    private void serviceTask() {

        dbHelper = new DBHelper(this);


        new Thread(new Runnable() {
            public void run() {
                while (!NetworkServiceStop) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    pairs = new ArrayList<NameValuePair>();

                    responseStr="-";

                    Cursor c = db.query("points", null, null, null, null, null, null);

                    // ставим позицию курсора на первую строку выборки
                    // если в выборке нет строк, вернется false
                    if (c.moveToFirst()) {

                        // определяем номера столбцов по имени в выборке
                        int idColIndex = c.getColumnIndex("id");
                        int dateColIndex = c.getColumnIndex("date");
                        int latColIndex = c.getColumnIndex("lat");
                        int lonColIndex = c.getColumnIndex("lon");
                        int typeColIndex = c.getColumnIndex("type");

                        do {
                            // получаем значения по номерам столбцов и пишем все в лог

                            row_id=c.getInt(idColIndex);
                            date=c.getString(dateColIndex);
                            lat=c.getString(latColIndex);
                            lon=c.getString(lonColIndex);
                            type=c.getString(typeColIndex);

                            pairs.add(new BasicNameValuePair("dev_id", deviceID));
                            pairs.add(new BasicNameValuePair("date", date));
                            pairs.add(new BasicNameValuePair("lat", lat));
                            pairs.add(new BasicNameValuePair("lon", lon));
                            pairs.add(new BasicNameValuePair("type", type));

                            try {
                                post.setEntity(new UrlEncodedFormEntity(pairs));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            try {
                                response = client.execute(post);
                                responseStr = EntityUtils.toString(response.getEntity());
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.d("NetWork", "Network Error");
                            }
                            if(responseStr.equals("0"))
                            {
                                int delCount = db.delete("points", "id = " + row_id, null);
                                Log.d("DELETED", "deleted rows count = " + delCount);
                            }
                            // переход на следующую строку
                            // а если следующей нет (текущая - последняя), то false - выходим из цикла
                        } while (c.moveToNext());

                        c.close();
                    }
                    else
                    {
                        if(TracerServiceStop) {
                            NetworkServiceStop = true;
                            unregisterReceiver(br);
                            stopSelf();
                        }
                    }


                    try {
                        Thread.sleep(120*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }


    @Override
    public IBinder onBind(Intent intent) {
        String NSS = intent.getStringExtra("NSS");

        if (NSS != null) {if(NSS.equals("stop"))
            NetworkServiceStop=true;
        }
        throw new UnsupportedOperationException("Not yet implemented");
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

            db.execSQL("create table points_storage ("
                    + "id integer primary key autoincrement,"
                    + "date text,"
                    + "lat text,"
                    + "lon text,"
                    + "type text" + ");");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
