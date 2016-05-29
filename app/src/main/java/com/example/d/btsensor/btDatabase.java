package com.example.d.btsensor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

/**
 * Created by d on 28/05/16.
 */
public class btDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "btTemperature.db";
    public static final String TABLE_NAME = "sensor";
    public static final String SENSOR_DATAPOINT = "datapoint";
    public static final String SENSOR_ID = "id";
    public static final String SENSOR_TEMPERATURE = "temperature";
    public static final String SENSOR_HUMIDITY = "humidity";

    public btDatabase(Context context){
        super(context,DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(
                "create table " + TABLE_NAME +
                        "(id integer primary key, datapoint integer, temperature real, humidity real)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertDataPoint(int datapoint, double temperature, double humidity){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("datapoint", datapoint);
        cv.put("temperature",temperature);
        cv.put("humidity", humidity);
        db.insert(TABLE_NAME, null, cv);
        Log.i("SQL Insert", "Inserted datapoint");
        return true;
    }

    public void clearDatabase(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
    public int getLatestPoint(){
        SQLiteDatabase db = this.getReadableDatabase();
        return (int)DatabaseUtils.queryNumEntries(db,TABLE_NAME);
    }

    public DataPoint[] getAllData(){
        DataPoint[] dp;
        int numRows;
        double temp;
        SQLiteDatabase db = this.getReadableDatabase();
        numRows = (int)DatabaseUtils.queryNumEntries(db,TABLE_NAME);
        dp = new DataPoint[numRows];
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " ORDER BY datapoint ASC", null);
        res.moveToFirst();

        for(int i = 0; i<numRows; i++){
            temp = res.getDouble(res.getColumnIndex(SENSOR_TEMPERATURE));
            DataPoint tdp = new DataPoint(i,temp);
            dp[i] = tdp;
            res.moveToNext();
        }
        Log.i("SQLite Num of rows", String.valueOf(numRows));
        return dp;
    }
}
