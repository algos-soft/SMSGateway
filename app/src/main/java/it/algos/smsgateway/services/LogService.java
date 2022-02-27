package it.algos.smsgateway.services;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;

import it.algos.smsgateway.AppContainer;
import it.algos.smsgateway.Constants;
import it.algos.smsgateway.R;
import it.algos.smsgateway.SmsGatewayApp;
import it.algos.smsgateway.logging.LogDbHelper;
import it.algos.smsgateway.logging.LogItem;
import it.algos.smsgateway.logging.LogItemModel;
import it.algos.smsgateway.mail.GMailService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LogService {

    private LogDbHelper logDatabase;

    private Context context;

    private Gson gson;

    private OkHttpClient okHttpClient;

    public LogService(Context context) {

        this.context = context;

        logDatabase = new LogDbHelper(context);

        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        this.gson = appContainer.gson;
        this.okHttpClient = appContainer.okHttpClient;

    }


    public void logE(Exception ex) {
        Log.e(Constants.LOG_TAG, null, ex);
        commonlog("E", null, ex);
    }

    public void logE(String msg, Exception ex) {
        Log.e(Constants.LOG_TAG, msg, ex);
        commonlog("E", msg, ex);
    }

    public void logI(String msg) {
        Log.i(Constants.LOG_TAG, msg);
        commonlog("I", msg, null);
    }

    public void logD(String msg) {
        Log.d(Constants.LOG_TAG, msg);
        commonlog("D", msg, null);
    }


    private void commonlog(String lvl, String msg, Exception ex) {

        LogItem logItem = new LogItem(LocalDateTime.now(), lvl, msg, ex);

        // internal log rotation - delete old items
        logDatabase.limitItems();

        // add new item to internal log db
        logDatabase.insertItem(logItem);

        // send the log item to the server
        try {
            notifyNewLogItem(logItem);
        } catch (IOException e) {
            Log.w(Constants.LOG_TAG, "Could not send log item to the server");
        }

        // if error, notify the admins via email
        if(lvl.equals("E")){

            LogItemModel model = convertLogItem(logItem);
            StringBuilder sb = new StringBuilder();

            if(!TextUtils.isEmpty(model.getTimestamp())){
                sb.append("timestamp:\n"+model.getTimestamp());
            }

            if(!TextUtils.isEmpty(model.getMessage())){
                if(sb.length()>0){
                    sb.append("\n\n");
                }
                sb.append("message:\n"+model.getMessage());
            }

            if(!TextUtils.isEmpty(model.getStacktrace())){
                if(sb.length()>0){
                    sb.append("\n\n");
                }
                sb.append("stacktrace:\n"+model.getStacktrace());
            }

            getGMailService().sendMail("SMS GATEWAY ERROR", sb.toString());

        }


    }


    private static LogItemModel convertLogItem(LogItem source) {
        LogItemModel dest = new LogItemModel();
        dest.setTimestamp(source.getTime().toString());
        dest.setLevel(source.getLvl());
        dest.setMessage(source.getMsg());

        if (source.getEx() != null) {
            dest.setMessage(source.getEx().getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            source.getEx().printStackTrace(pw);
            dest.setStacktrace(sw.toString());
        }

        return dest;
    }


    public String getLog() {
        return logDatabase.getItemsAsText();
    }

    public void clearLog() {
        logDatabase.clearDatabase();
    }


    /**
     * Notify the server that a new log item has been created
     * <br>
     *
     * @param item the log item for the server
     */
    private void notifyNewLogItem(LogItem item) throws IOException {

        LogItemModel svrItem = convertLogItem(item);

        String url = getUtilsService().buildUrl(context, "messages/log");

        String json = gson.toJson(svrItem);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        String token = getPrefsService().getString(R.string.token);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + token)
                .post(body)
                .build();

        Call call = okHttpClient.newCall(request);

        if (Looper.myLooper() == Looper.getMainLooper()) {    // we are in the main thread, go async

            call.enqueue(new Callback() {

                public void onResponse(Call call, Response response) throws IOException {

                    if (!response.isSuccessful()) {

                        int code = response.code();

                        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {

                            // obtain a new token and call this method recursively
                            getAuthService().refreshToken(false);
                            notifyNewLogItem(item);

                        } else {
                            String reason = response.body().string();
                            throw new IOException("http code " + code + ": " + reason);
                        }
                    }

                }

                public void onFailure(Call call, IOException e) {
                    Log.e(Constants.LOG_TAG, "call to server to notify a new log has failed", e);
                }

            });

        } else {  // we are already in a background thread, stay sync

            Response response = call.execute();

            if (!response.isSuccessful()) {

                int code = response.code();

                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {

                    // obtain a new token and call this method recursively
                    getAuthService().refreshToken(false);
                    notifyNewLogItem(item);

                } else {
                    String reason = response.body().string();
                    throw new IOException("http code " + code + ": " + reason);
                }
            }


        }


//        Response response = okHttpClient.newCall(request).execute();
//
//        if (!response.isSuccessful()) {
//
//            int code = response.code();
//
//            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
//
//                // obtain a new token and call this method recursively
//                getAuthService().refreshToken(false);
//                notifyNewLogItem(item);
//
//            } else {
//                String reason = response.body().string();
//                throw new IOException("http code " + code + ": " + reason);
//            }
//        }


    }


    private UtilsService getUtilsService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getUtilsService();
    }

    private AuthService getAuthService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getAuthService();
    }

    private PrefsService getPrefsService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getPrefsService();
    }

    public GMailService getGMailService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getGMailService();
    }


}
