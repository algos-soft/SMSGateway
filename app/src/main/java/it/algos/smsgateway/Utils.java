package it.algos.smsgateway;

import android.content.Context;
import android.util.Log;

import java.time.LocalDateTime;

public class Utils {




    public static String buildUrl(Context context, String path) {

        String protocol;
        if (Prefs.getBoolean(context, R.string.usessl)) {
            protocol = "https";
        }else{
            protocol = "http";
        }

        String host=Prefs.getString(context, R.string.host);

        String port=Prefs.getString(context, R.string.port);

        String url = protocol + "://" + host + ":" + port + "/" + path;

        return url;

    }

    public static void logE(Exception ex) {
        SmsGatewayApp.log("E", null, ex);
    }

    public static void logE(String msg, Exception ex) {
        SmsGatewayApp.log("E", msg, ex);
    }


    public static void logI(String msg){
        SmsGatewayApp.log("I", msg, null);
    }



}
