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
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import it.algos.smsgateway.Prefs;
import it.algos.smsgateway.R;
import it.algos.smsgateway.LogUtils;
import it.algos.smsgateway.Utils;
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

    private Utils utils;

    public GatewayWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();

        gson = new Gson();

        utils=new Utils();

    }



    @NonNull
    @Override
    public Result doWork() {

        List<Message> messages=null;
        try {
            messages = queryMessages();
        } catch (IOException e) {
            LogUtils.logE(e);
        }

        sendMessages(messages);

        return Result.success();

    }


    /**
     * Query the server for the list of messages to send.
     * If the token is missing or expired, refresh the token and call this method again.
     * @return the list of the messages to send .
     */
    private List<Message> queryMessages() throws IOException {

        String url = utils.buildUrl(getApplicationContext(), "messages/pending");

        String token = Prefs.getString(getApplicationContext(), R.string.token);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + token)
                .get()
                .build();

        LogUtils.logI("Sending request to " + url);

        Response response = client.newCall(request).execute();

        LogUtils.logI("Response received from " + url);

        List<Message> messages = new ArrayList<>();

        if (response.isSuccessful()) {

            String json = response.body().string();

            Message[] amsg = gson.fromJson(json, Message[].class);
            for (Message msg : amsg) {
                messages.add(msg);
            }

            LogUtils.logI("Received " + messages.size() + " messages from server");

        } else {
            int code = response.code();

            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {

                // obtain a new token and call this method recursively
                refreshToken();
                queryMessages();

            } else {
                String reason = response.body().string();
                throw new IOException("http code " + code + ": " + reason);
            }
        }

        return messages;

    }


    /**
     * Obtain a new token from the server and store it in the application preference storage
     */
    private void refreshToken() throws IOException {

        String url = utils.buildUrl(getApplicationContext(), "messages/token");

        String apiKey = Prefs.getString(getApplicationContext(), R.string.apikey);

        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), apiKey);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        LogUtils.logI("Sending request to " + url);

        Response response = client.newCall(request).execute();

        LogUtils.logI("Response " + response.code() + " received from " + url);

        if (response.isSuccessful()) {
            String sResp = response.body().string();
            Prefs.putString(getApplicationContext(), R.string.token, sResp);
            LogUtils.logI("Token refreshed successfully");

        }

    }


    /**
     * Deliver all the messages in the list.
     * Pauses some time between messages.
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
                LogUtils.logE(ex);
            }

            SystemClock.sleep(4000);

        }
    }


    /**
     * Send a single SMS
     */
    public void sendSMS(String phoneNo, String msg, String id) throws InvalidSmsException {

        // validate phone number
        String validNumber;
        try {
            Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber(phoneNo);
            validNumber=""+phoneNumber.getNationalNumber();
        } catch (NumberParseException e) {
            throw new InvalidSmsException("Invalid phone number: "+phoneNo, e);
        }

        // validate message length
        if (msg.length() > 160) {
            throw new InvalidSmsException("SMS text too long: " + msg.length() + " (max is 160)");
        }

        // send the SMS
        try {

            SmsManager smsManager = SmsManager.getDefault();

            smsManager.sendTextMessage(validNumber, null, msg, null, null);
            LogUtils.logI("SMS sent to # " + validNumber + ": " + msg);

            // update counters in the preferences storage
            int numSms = Prefs.getInt(getApplicationContext(), R.string.pref_numsms);
            numSms++;
            Prefs.putInt(getApplicationContext(), R.string.pref_numsms, numSms);

            String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            Prefs.putString(getApplicationContext(), R.string.pref_date_last_sms, currentDate);

            setProgressAsync(new Data.Builder().build());

            notifyMessageSent(id);


        } catch (Exception ex) {

            LogUtils.logE(ex);

        }
    }


    /**
     * Notify the server that a message has been sent successfully
     * <br>
     *
     * @param id id of the message
     */
    private void notifyMessageSent(String id) throws IOException {

        String url = utils.buildUrl(getApplicationContext(), "messages/sent");

        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), id);

        String token = Prefs.getString(getApplicationContext(), R.string.token);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + token)
                .post(body)
                .build();

        LogUtils.logI("Sending request to " + url);

        Response response = client.newCall(request).execute();

        LogUtils.logI("Response " + response.code() + " received from " + url);

        if (response.isSuccessful()) {

            LogUtils.logI("Message sent with id " + id + " notified successfully to server");

        } else {

            int code = response.code();

            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {

                // obtain a new token and call this method recursively
                refreshToken();
                notifyMessageSent(id);

            } else {
                String reason = response.body().string();
                throw new IOException("http code " + code + ": " + reason);
            }
        }


    }


}
