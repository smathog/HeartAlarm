package com.scottstuff.heartalarm.SQL;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class to manage the creation of a new table in SQLite to save a HR recording
 */
public class HrSQLiteRecorder {
    private final String TABLE_NAME;
    private static final String TIME_FIELD = "Timestamp";
    private static final String HR_FIELD = "BPM";


    public HrSQLiteRecorder(Context context, Long firstTimestamp) {
        Date date = new Date(firstTimestamp);
        DateFormat format = new SimpleDateFormat("yyyy.MM.dd:HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.TABLE_NAME = "HR" + format.format(date);

        HrSQLiteManager manager = HrSQLiteManager.getInstance(context);
        SQLiteDatabase db = manager.getWritableDatabase();
        StringBuilder sql = new StringBuilder()
                .append("CREATE TABLE IF NOT EXISTS ")
                .append(TABLE_NAME)
                .append("(")
                .append(TIME_FIELD)
                .append(" INTEGER PRIMARY KEY, ")
                .append(HR_FIELD)
                .append(" INT)");
        db.execSQL(sql.toString());
    }
}
