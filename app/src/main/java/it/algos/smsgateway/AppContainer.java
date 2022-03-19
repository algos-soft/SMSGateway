package it.algos.smsgateway;

import android.content.Context;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import it.algos.smsgateway.mail.GMailService;
import it.algos.smsgateway.services.AuthService;
import it.algos.smsgateway.services.LogService;
import it.algos.smsgateway.services.PrefsService;
import it.algos.smsgateway.services.UtilsService;
import okhttp3.OkHttpClient;

// Container of objects shared across the whole app for dependency injection
public class AppContainer {

    private Context context;

    // these services do not have cross dependencies,
    // they are created early and can be accessed directly
    public OkHttpClient okHttpClient;
    public Gson gson;


    // these services can have cross dependencies
    // they are subject to lazy creation
    // use getters to obtain them
    private UtilsService utilsService;
    private LogService logService;
    private AuthService authService;
    private PrefsService prefsService;
    private GMailService gMailService;


    public AppContainer(Context context) {

        this.context = context;

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();

        gson = new Gson();

    }


    public LogService getLogService() {
        if (logService != null) {
            return logService;
        }
        logService = new LogService(context);
        return logService;
    }

    public AuthService getAuthService() {
        if (authService != null) {
            return authService;
        }
        authService = new AuthService(context);
        return authService;
    }

    public UtilsService getUtilsService() {
        if (utilsService != null) {
            return utilsService;
        }
        utilsService = new UtilsService(context);
        return utilsService;
    }

    public PrefsService getPrefsService() {
        if (prefsService != null) {
            return prefsService;
        }
        prefsService = new PrefsService(context);
        return prefsService;
    }

    public GMailService getGMailService() {
        if (gMailService != null) {
            return gMailService;
        }
        gMailService = new GMailService(context);
        return gMailService;
    }



}
