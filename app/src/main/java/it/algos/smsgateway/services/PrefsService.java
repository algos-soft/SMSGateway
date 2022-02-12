package it.algos.smsgateway.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefsService {

    private Context context;

    public PrefsService(Context context) {
        this.context = context;
    }

    private SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getString(int key) {
        String sKey = context.getString(key);
        return getPreferences(context).getString(sKey, "");
    }

    public void putString(int key, String value) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(context.getString(key), value).apply();
    }

    public int getInt(int key) {
        String sKey = context.getString(key);
        return getPreferences(context).getInt(sKey, 0);
    }

    public void putInt(int key, int value) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(context.getString(key), value).apply();
    }

    public boolean getBoolean(int key) {
        String sKey = context.getString(key);
        return getPreferences(context).getBoolean(sKey, false);
    }

    public void putBoolean(int key, boolean value) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(context.getString(key), value).apply();
    }

}
