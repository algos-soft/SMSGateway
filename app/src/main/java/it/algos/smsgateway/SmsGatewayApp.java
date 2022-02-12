package it.algos.smsgateway;

import android.app.Application;

public class SmsGatewayApp extends Application {

    // Instance of AppContainer that will be used for dependency injection
    public AppContainer appContainer;

    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(getApplicationContext());
    }

}
