package it.algos.smsgateway;

import android.content.Context;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Query the server for the list of the SMS to send<br>
 * Send the SMS in the same order as received by the server<br>
 * POST a confirmation to the server after each SMS is sent successfully.
 */
public class QuerySendAndConfirmWorker extends Worker {


    private OkHttpClient client;

    private List<Message> messages;

    private String token;

    private Gson gson;


    public QuerySendAndConfirmWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        client = new OkHttpClient();
        messages = new ArrayList<>();
        token = "";
        gson = new Gson();
    }

    @NonNull
    @Override
    public Result doWork() {

        try {
            queryMessages();
        } catch (IOException e) {
            Utils.logException(e);
        }

        sendMessages();

        return Result.success();

    }


    /**
     * Query the server for the list of messages to send.
     * If the token is missing or expired, refresh the token and call this method again.
     * Fills the list of the messages to send (instance variable).
     */
    private void queryMessages() throws IOException {

        String url = Utils.buildUrl(getApplicationContext(), "messages/pending");

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + token)
                .get()
                .build();

        Log.i(Constants.LOG_TAG, "Sending request to " + url);

        Response response = client.newCall(request).execute();

        Log.i(Constants.LOG_TAG, "Response received from " + url);

        if (response.isSuccessful()) {

            String json = response.body().string();
            fillMessageList(json);
            Log.i(Constants.LOG_TAG, "Received " + messages + " messages from server");

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

    }


    /**
     * Obtain a new token from the server and store it in the instance variable
     */
    private void refreshToken() throws IOException {

        String url = Utils.buildUrl(getApplicationContext(),"messages/token");

        String apiKey = Prefs.getString(getApplicationContext(), R.string.apikey);

        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), apiKey);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Log.i(Constants.LOG_TAG, "Sending request to " + url);

        Response response = client.newCall(request).execute();

        Log.i(Constants.LOG_TAG, "Response "+response.code()+" received from " + url);

        if (response.isSuccessful()) {
            String sResp = response.body().string();
            token = sResp;
            Log.i(Constants.LOG_TAG, "Token refreshed successfully");
        }

    }


    /**
     * Clear the messages list and fill it with the query results
     */
    private void fillMessageList(String json) {
        Message[] amsg = gson.fromJson(json, Message[].class);
        messages.clear();
        for (Message msg : amsg) {
            messages.add(msg);
        }
    }

    /**
     * Deliver all the messages in the list.
     * Pauses some time between messages.
     */
    private void sendMessages() {

        for (Message msg : messages) {

            String phone = msg.getPhone();
            String text = msg.getMessage();
            String id = msg.getId();
            sendSMS(phone, text, id);

            SystemClock.sleep(4000);

        }
    }


    /**
     * Send a single SMS
     */
    public void sendSMS(String phoneNo, String msg, String id) {
        try {

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);

            // update counters in the preferences storage
            int numSms = Prefs.getInt(getApplicationContext(), R.string.pref_numsms);
            numSms++;
            Prefs.putInt(getApplicationContext(), R.string.pref_numsms, numSms);

            String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            Prefs.putString(getApplicationContext(), R.string.pref_date_last_sms, currentDate);

            setProgressAsync(new Data.Builder().build());

            notifyMessageSent(id);

            Log.i(Constants.LOG_TAG, "SMS sent to # " + phoneNo + ": " + msg);

        } catch (Exception ex) {

            Utils.logException(ex);

        }
    }


    /**
     * Notify the server that a message has been sent successfully
     * <br>
     * @param id id of the message
     */
    private void notifyMessageSent(String id) throws IOException {

        String url = Utils.buildUrl(getApplicationContext(),"messages/sent");

        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), id);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + token)
                .post(body)
                .build();

        Log.i(Constants.LOG_TAG, "Sending request to " + url);

        Response response = client.newCall(request).execute();

        Log.i(Constants.LOG_TAG, "Response "+response.code()+" received from " + url);

        if (response.isSuccessful()) {

            Log.i(Constants.LOG_TAG, "Message sent with id " + id + " notified successfully to server");

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
