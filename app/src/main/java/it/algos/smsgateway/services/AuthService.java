package it.algos.smsgateway.services;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import it.algos.smsgateway.AppContainer;
import it.algos.smsgateway.Constants;
import it.algos.smsgateway.R;
import it.algos.smsgateway.SmsGatewayApp;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthService {

    private Context context;

    private OkHttpClient okHttpClient;

    public AuthService(Context context) {
        this.context = context;

        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        this.okHttpClient = appContainer.okHttpClient;

    }

    /**
     * Obtain a new token from the server and store it in the application preference storage
     */
    public void refreshToken() throws IOException {
        refreshToken(true);
    }


    /**
     * Obtain a new token from the server and store it in the application preference storage
     * <br>
     *
     * @param fullLog true to fully log the operations (db + http to server), false for local logging only
     */
    public void refreshToken(boolean fullLog) throws IOException {

        String msg;

        String url = getUtilsService().buildUrl(context, "messages/token");

        String apiKey = getPrefsService().getString(R.string.apikey);

        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), apiKey);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        msg = "Sending request to " + url;
        if (fullLog) {
            getLogService().logI(msg);
        } else {
            Log.i(Constants.LOG_TAG, msg);
        }

        Response response = okHttpClient.newCall(request).execute();

        msg = "Response " + response.code() + " received from " + url;
        if (fullLog) {
            getLogService().logI(msg);
        } else {
            Log.i(Constants.LOG_TAG, msg);
        }

        if (response.isSuccessful()) {
            String sResp = response.body().string();
            getPrefsService().putString(R.string.token, sResp);

            msg = "Token refreshed successfully";
            if (fullLog) {
                getLogService().logI(msg);
            } else {
                Log.i(Constants.LOG_TAG, msg);
            }
        }

    }


    public LogService getLogService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getLogService();
    }

    public UtilsService getUtilsService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getUtilsService();
    }

    public PrefsService getPrefsService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getPrefsService();
    }


}
