package it.algos.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Prefs {

    public static SharedPreferences getPreferences(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    };

    public static String getString(Context context, int key){
        String sKey = context.getString(key);
        return getPreferences(context).getString(sKey,"");
    };

    public static void putString(Context context, int key, String value){
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(context.getString(key), value).apply();
    };

    public static int getInt(Context context, int key){
        String sKey = context.getString(key);
        return getPreferences(context).getInt(sKey,0);
    };

    public static void putInt(Context context, int key, int value){
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(context.getString(key), value).apply();
    };


}
