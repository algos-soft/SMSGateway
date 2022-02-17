package it.algos.smsgateway.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsSentBroadcastReceiver extends BroadcastReceiver {


    // The mobile device has sent the SMS to the SMSC (Short message service center).
    // And the SMSC has confirmed it has received the SMS.
    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("id");
        int a = 87;
        int b=a;
    }
}
