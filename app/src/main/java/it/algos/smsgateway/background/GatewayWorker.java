package it.algos.smsgateway.background;

import android.content.Context;
import android.os.SystemClock;
import android.telephony.SmsManager;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.algos.smsgateway.AppContainer;
import it.algos.smsgateway.services.AuthService;
import it.algos.smsgateway.services.PrefsService;
import it.algos.smsgateway.R;
import it.algos.smsgateway.services.LogService;
import it.algos.smsgateway.SmsGatewayApp;
import it.algos.smsgateway.services.UtilsService;
import it.algos.smsgateway.exceptions.InvalidSmsException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Query the server for the list of the SMS to send<br>
 * Send the SMS in the same order as received by the server<br>
 * POST a confirmation to the server after each SMS is sent successfully.
 * <br>
 * WARNING: Android creates a new instance of this object every time it is
 * called by the system scheduler.
 */
public class GatewayWorker extends Worker {

    private OkHttpClient client;

    private Gson gson;


    public GatewayWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // do injections
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        this.client = appContainer.okHttpClient;
        this.gson = appContainer.gson;


        List<Message> messages = null;
        try {
            messages = queryMessages();
        } catch (IOException e) {
            getLogService().logE(e);
        }

        sendMessages(messages);

        return Result.success();

    }


    /**
     * Query the server for the list of messages to send.
     * If the token is missing or expired, refresh the token and call this method again.
     *
     * @return the list of the messages to send .
     */
    private List<Message> queryMessages() throws IOException {

        String url = getUtilsService().buildUrl(getApplicationContext(), "messages/pending");

        String token = getPrefsService().getString(R.string.token);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + token)
                .get()
                .build();

        getLogService().logI("Sending request to " + url);

        Response response = client.newCall(request).execute();

        getLogService().logI("Response received from " + url);

        List<Message> messages = new ArrayList<>();

        if (response.isSuccessful()) {

            String json = response.body().string();

            Message[] amsg = gson.fromJson(json, Message[].class);
            for (Message msg : amsg) {
                messages.add(msg);
            }

            getLogService().logI("Received " + messages.size() + " messages from server");

        } else {
            int code = response.code();

            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {

                // obtain a new token and call this method recursively
                getAuthService().refreshToken();
                queryMessages();

            } else {
                String reason = response.body().string();
                throw new IOException("http code " + code + ": " + reason);
            }
        }

        return messages;

    }


    /**
     * Deliver all the messages in the list.
     * Pauses some time between messages.
     *
     * @param messages list of messages to send
     */
    private void sendMessages(List<Message> messages) {

        for (Message msg : messages) {

            String phone = msg.getPhone();
            String text = msg.getMessage();
            String id = msg.getId();

            try {

                sendSMS(phone, text, id);

            } catch (InvalidSmsException ex) {
                getLogService().logE(ex);
            }

            SystemClock.sleep(4000);

        }
    }


    /**
     * Send a single SMS
     */
    public void sendSMS(String phoneNo, String msg, String id) throws InvalidSmsException {

        getLogService().logD("SMS transmission requested: #=" + phoneNo + ", text=" + msg + ", id=" + id);

        // validate phone number
        String validNumber;
        try {
            Phonenumber.PhoneNumber phoneNumber = getUtilsService().validatePhoneNumber(phoneNo);
            validNumber = "" + phoneNumber.getNationalNumber();
            getLogService().logD("phone number validation passed for " + phoneNo + ", parsed number is=" + validNumber);
        } catch (NumberParseException e) {
            throw new InvalidSmsException("Invalid phone number: " + phoneNo, e);
        }

        // validate message length
        if (msg.length() > 160) {
            throw new InvalidSmsException("SMS text too long: " + msg.length() + " (max is 160)");
        } else {
            getLogService().logD("message length validation passed: length=" + msg.length());
        }

        // send the SMS
        try {

            SmsManager smsManager = SmsManager.getDefault();

            getLogService().logD("invoking SmsManager for " + validNumber + ", msg length=" + msg.length());

            smsManager.sendTextMessage(validNumber, null, msg, null, null);

            getLogService().logI("SMS sent to # " + validNumber + ": " + msg);

            // update counters in the preferences storage
            int numSms = getPrefsService().getInt(R.string.pref_numsms);
            numSms++;
            getPrefsService().putInt(R.string.pref_numsms, numSms);

            String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            getPrefsService().putString(R.string.pref_date_last_sms, currentDate);

            setProgressAsync(new Data.Builder().build());

            notifyMessageSent(id);


        } catch (Exception ex) {

            getLogService().logE(ex);

        }
    }


    /**
     * Notify the server that a message has been sent successfully
     * <br>
     *
     * @param id id of the message
     */
    private void notifyMessageSent(String id) throws IOException {

        String url = getUtilsService().buildUrl(getApplicationContext(), "messages/sent");

        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), id);

        String token = getPrefsService().getString(R.string.token);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + token)
                .post(body)
                .build();

        getLogService().logI("Sending request to " + url);

        Response response = client.newCall(request).execute();

        getLogService().logI("Response " + response.code() + " received from " + url);

        if (response.isSuccessful()) {

            getLogService().logI("Message sent with id " + id + " notified successfully to server");

        } else {

            int code = response.code();

            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {

                // obtain a new token and call this method recursively
                getAuthService().refreshToken();
                notifyMessageSent(id);

            } else {
                String reason = response.body().string();
                throw new IOException("http code " + code + ": " + reason);
            }
        }


    }


    public LogService getLogService() {
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        return appContainer.getLogService();
    }

    public AuthService getAuthService() {
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        return appContainer.getAuthService();
    }

    public UtilsService getUtilsService() {
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        return appContainer.getUtilsService();
    }

    public PrefsService getPrefsService() {
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        return appContainer.getPrefsService();
    }


}
