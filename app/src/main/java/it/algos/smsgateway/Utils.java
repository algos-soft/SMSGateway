package it.algos.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Utils {

    public static void logException(Exception ex){
        Log.e(Constants.LOG_TAG, "exception", ex);
    }


    public static String buildUrl(String path)
    {

        final String protocol = "http";
        final String host = "192.168.0.6";
        final String port = "8080";

        String url = protocol + "://" + host + ":" + port + "/" + path;

        return url;

    }


}
