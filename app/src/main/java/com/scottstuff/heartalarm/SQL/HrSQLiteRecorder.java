package com.scottstuff.heartalarm.SQL;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.scottstuff.heartalarm.DataTypes.HRData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class to manage the creation of a new table in SQLite to save a HR recording
 */
public class HrSQLiteRecorder {
    private final String TABLE_NAME;
    private static final String TIME_FIELD = "Timestamp";
    private static final String HR_FIELD = "BPM";

    // Size at which queue data should be inserted into table
    private final int insertThreshold;

    // Reference to writeable database
    private final SQLiteDatabase db;

    // Concurrent blocking queue to handle incoming and outgoing data
    private final LinkedBlockingQueue<HRData> queue;
    // Single-threaded executor for handling inserts into table
    private final ExecutorService executorService;
    // Future for checking if insertion is done
    private Future<?> insertion;


    public HrSQLiteRecorder(Context context, Long firstTimestamp, int insertThreshold) {
        Date date = new Date(firstTimestamp);
        DateFormat format = new SimpleDateFormat("yyyy.MM.dd:HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.TABLE_NAME = "HR" + format.format(date);

        HrSQLiteManager manager = HrSQLiteManager.getInstance(context);
        this.db = manager.getWritableDatabase();
        StringBuilder sql = new StringBuilder()
                .append("CREATE TABLE IF NOT EXISTS ")
                .append(TABLE_NAME)
                .append("(")
                .append(TIME_FIELD)
                .append(" INTEGER PRIMARY KEY, ")
                .append(HR_FIELD)
                .append(" INT)");
        db.execSQL(sql.toString());

        this.insertThreshold = insertThreshold;
        queue = new LinkedBlockingQueue<>();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insertRecording(HRData dataPoint) {
        queue.add(dataPoint);
        if ((insertion == null || insertion.isDone()) && queue.size() >= insertThreshold) {
            insertion = executorService.submit(insertIntoTable());
        }
    }

    private Runnable insertIntoTable() {
        return () -> {
            try {
                db.beginTransaction();
                for (int i = 0; i < insertThreshold; ++i) {
                    HRData data = queue.take();
                    ContentValues cv = new ContentValues();
                    cv.put(TIME_FIELD, data.getTimeStamp());
                    cv.put(HR_FIELD, data.getBpm());
                    db.insert(TABLE_NAME, null, cv);
                }
                db.setTransactionSuccessful();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        };
    }
}
