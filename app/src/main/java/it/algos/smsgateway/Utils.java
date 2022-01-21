package it.algos.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Utils {

    public static void logException(Exception ex) {
        Log.e(Constants.LOG_TAG, "exception", ex);
    }


    public static String buildUrl(Context context, String path) {

        String protocol;
        if (Prefs.getBoolean(context, R.string.usessl)) {
            protocol = "https";
        }else{
            protocol = "http";
        }

        String host=Prefs.getString(context, R.string.host);

        String port=Prefs.getString(context, R.string.port);

//        protocol = "http";
//        host = "192.168.0.6";
//        port = "8080";

        String url = protocol + "://" + host + ":" + port + "/" + path;

        return url;

    }


}
