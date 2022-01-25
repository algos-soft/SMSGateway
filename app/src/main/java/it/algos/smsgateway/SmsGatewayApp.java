package it.algos.smsgateway;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SmsGatewayApp extends Application {

    private static LogDbHelper logDatabase;

    private static Context context;

    private static final String LOG_PREFS_NAME = "it.algos.smsgateway.log_prefs";

    public void onCreate() {
        super.onCreate();
        SmsGatewayApp.context = getApplicationContext();
        logDatabase = new LogDbHelper(context);
    }

    public static Context getAppContext() {
        return SmsGatewayApp.context;
    }

    public static void log(String lvl, String msg, Exception ex) {

        switch (lvl) {
            case "I":
                Log.i(Constants.LOG_TAG, msg);
                break;
            case "D":
                Log.d(Constants.LOG_TAG, msg);
                break;
            case "E":
                Log.e(Constants.LOG_TAG, msg, ex);
                break;
        }

//        // rotate internal log
//        // (remove all elements exceeding max log size)
//        int max = Constants.MAX_LOG_ITEMS - 1;
//        if (logItems.size() > max) {
//            List<LogItem> toBeRemoved = logItems.subList(max, logItems.size());
//            for (LogItem item : toBeRemoved) {
//                logItems.remove(item);
//            }
//        }

        LogItem logItem = new LogItem(LocalDateTime.now(), lvl, msg, ex);
//        logItems.add(0, logItem);

        // log rotation - delete old items
        logDatabase.limitItems();


        logDatabase.insertItem(logItem);

    }

    public static String getLog() {
//        StringBuilder sb = new StringBuilder();
//        for(LogItem item : logItems){
//            sb.append(item.getString());
//            sb.append("\n");
//        }
//        return sb.toString();

//        SharedPreferences logStorage = context.getSharedPreferences(LOG_PREFS_NAME, MODE_PRIVATE);
//        Map<String, ?> allEntries = logStorage.getAll();
//        StringBuilder sb = new StringBuilder();
//        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//            String string = entry.getValue().toString();
//            sb.append(string);
//            sb.append("\n");
//        }
//        return sb.toString();


        return logDatabase.getItemsAsText();

    }

    public static void clearLog() {
        logDatabase.clearDatabase();
    }



}
