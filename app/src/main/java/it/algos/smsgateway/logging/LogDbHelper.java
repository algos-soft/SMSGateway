package it.algos.smsgateway.logging;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import it.algos.smsgateway.Constants;


public class LogDbHelper extends SQLiteOpenHelper {

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LogDbContract.LogEntry.TABLE_NAME + " (" +
                    LogDbContract.LogEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    LogDbContract.LogEntry.COLUMN_TIMESTAMP + " TEXT," +
                    LogDbContract.LogEntry.COLUMN_LEVEL + " TEXT," +
                    LogDbContract.LogEntry.COLUMN_MSG + " TEXT," +
                    LogDbContract.LogEntry.COLUMN_EXCEPTION + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LogDbContract.LogEntry.TABLE_NAME;


    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "log_db";


    public LogDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL(SQL_DELETE_ENTRIES);
        // Create tables again
        onCreate(db);
    }


    public long insertItem(LogItem logItem) {

        // get writable database as we want to write data
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        // `id` will be inserted automatically, no need to add it
        values.put(LogDbContract.LogEntry.COLUMN_TIMESTAMP, logItem.getTime().toString());
        values.put(LogDbContract.LogEntry.COLUMN_LEVEL, logItem.getLvl());
        values.put(LogDbContract.LogEntry.COLUMN_MSG, logItem.getMsg());

        Exception ex = logItem.getEx();
        if (ex != null) {
            Writer writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));
            values.put(LogDbContract.LogEntry.COLUMN_EXCEPTION, writer.toString());
        }

        // insert row
        long newRowId = db.insert(LogDbContract.LogEntry.TABLE_NAME, null, values);

        // close db connection
        db.close();

        // return newly inserted row id
        return newRowId;
    }


    public String getItemsAsText() {

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                LogDbContract.LogEntry.COLUMN_TIMESTAMP,
                LogDbContract.LogEntry.COLUMN_LEVEL,
                LogDbContract.LogEntry.COLUMN_MSG,
                LogDbContract.LogEntry.COLUMN_EXCEPTION
        };


        // How you want the results sorted in the resulting Cursor
        String sortOrder = LogDbContract.LogEntry._ID + " DESC";

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                LogDbContract.LogEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

        StringBuffer sb = new StringBuffer();
        while (cursor.moveToNext()) {
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract.LogEntry.COLUMN_TIMESTAMP));
            String level = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract.LogEntry.COLUMN_LEVEL));
            String msg = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract.LogEntry.COLUMN_MSG));
            String exception = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract.LogEntry.COLUMN_EXCEPTION));

            sb.append("[" + timestamp + "] " + level + ":");

            if (msg != null) {
                sb.append(" " + msg);
            }
            if (exception != null) {
                sb.append(" " + exception);
            }

            sb.append("\n");

        }
        cursor.close();

        return sb.toString();

    }


    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(LogDbContract.LogEntry.TABLE_NAME, null, null);
        db.close();
    }

    public void limitItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, LogDbContract.LogEntry.TABLE_NAME, null, null);

        if (count > Constants.MAX_LOG_ITEMS) {
            String query = "select * from " + LogDbContract.LogEntry.TABLE_NAME + " order by _id desc limit -1 offset " + Constants.MAX_LOG_ITEMS;
            Cursor cursor = db.rawQuery(query, null);
            List<Long> idsToDelete = new ArrayList<>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(LogDbContract.LogEntry._ID));
                idsToDelete.add(id);
            }
            cursor.close();

            idsToDelete.sort(null);

            SQLiteDatabase dbw = getWritableDatabase();
            for (long id : idsToDelete) {
                dbw.delete(
                        LogDbContract.LogEntry.TABLE_NAME,  // Where to delete
                        LogDbContract.LogEntry._ID + " = ?",
                        new String[]{"" + id}  // What to delete
                );
            }
            dbw.close();


        }


    }

}