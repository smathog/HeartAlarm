package com.scottstuff.heartalarm.SQL;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.scottstuff.heartalarm.DataTypes.HRData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Support class to help manage the creation and selection of tables in a SQLite database
 * corresponding to saved HR information.
 *
 * Note that this class is structured as a singleton, so use getInstance() to get a reference to
 * it.
 */
public class HrSQLiteManager extends SQLiteOpenHelper {
    private static HrSQLiteManager instance;

    private static final String DATABASE_NAME = "HrDB";
    private static final int DATABASE_VERSION = 1;

    private HrSQLiteManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static HrSQLiteManager getInstance(Context context) {
        if (instance == null) {
            instance = new HrSQLiteManager(context);
        }
        return instance;
    }

    public List<String> getTables() {
        try (Cursor cursor = instance.getReadableDatabase()
                .rawQuery("SELECT name " +
                        "FROM sqlite_master " +
                        "WHERE type='table' " +
                        "AND name LIKE 'HR%'", null)) {
            ArrayList<String> list = new ArrayList<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    list.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            }
            // Sort list in descending order
            list.sort(Comparator.reverseOrder());
            return list;
        }
    }

    public List<HRData> getData(String tableName) {
        String query = String.format("SELECT * FROM %s", tableName);
        try (Cursor cursor = instance.getReadableDatabase()
                .rawQuery(query, null)) {
            ArrayList<HRData> list = new ArrayList<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    long timestamp = cursor.getLong(cursor.getColumnIndex(HrSQLiteRecorder.getTimeField()));
                    int dataValue = cursor.getInt(cursor.getColumnIndex(HrSQLiteRecorder.getHrField()));
                    list.add(new HRData(timestamp, dataValue));
                    cursor.moveToNext();
                }
            }
            return list;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
