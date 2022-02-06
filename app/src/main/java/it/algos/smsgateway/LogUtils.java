package it.algos.smsgateway;

import android.content.Context;
import android.util.Log;

import java.time.LocalDateTime;

public class LogUtils {


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
