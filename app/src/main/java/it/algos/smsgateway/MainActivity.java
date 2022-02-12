package it.algos.smsgateway;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import it.algos.smsgateway.background.GatewayWorker;
import it.algos.smsgateway.logging.LogActivity;
import it.algos.smsgateway.services.LogService;
import it.algos.smsgateway.services.PrefsService;
import it.algos.smsgateway.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private static final String WORK_REQUEST_TAG = "it.algos.smsgateway.WORK_REQUEST";

    public MainActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.first_fragment_container_view, MainFragment.class, null)
                    .commit();
        }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        setSupportActionBar(toolbar);

        checkPreferenceDefaults();

        checkPermissions();

        observeWorker();

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        syncStatus();
    }


    /**
     * Fill empty preferences with default values
     */
    private void checkPreferenceDefaults() {
        String string;

        string = getPrefsService().getString(R.string.apikey);
        if (TextUtils.isEmpty(string)) {
            getPrefsService().putString(R.string.apikey, getString(R.string.apikey_default));
        }

        string = getPrefsService().getString(R.string.host);
        if (TextUtils.isEmpty(string)) {
            getPrefsService().putString(R.string.host, getString(R.string.host_default));
        }

        string = getPrefsService().getString(R.string.port);
        if (TextUtils.isEmpty(string)) {
            getPrefsService().putString(R.string.port, getString(R.string.port_default));
        }

        string = getPrefsService().getString(R.string.interval_minutes);
        if (TextUtils.isEmpty(string)) {
            getPrefsService().putString(R.string.interval_minutes, getString(R.string.interval_minutes_default));
        }

    }


    private void checkPermissions() {

        // check permission to send SMS
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            String[] permissionsArray = new String[]{Manifest.permission.SEND_SMS};
            requestPermissions(permissionsArray, 3);
        }

//        ActivityManager activitymanager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
//        boolean restricted=activitymanager.isBackgroundRestricted();

        // check permission to ignore battery optimization
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }

//        // Check if we have write storage permission (export log)
//        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//            int a = 87;
//            int b=a;
//
//            String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
//
//
//            // We don't have permission so prompt the user
//            ActivityCompat.requestPermissions(
//                    this,
//                    PERMISSIONS_STORAGE,
//                    1
//            );
//        }


    }


    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 3) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Showing the toast message
                Toast.makeText(MainActivity.this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();

                finishAndRemoveTask();

            }

        }

    }


    /**
     * Update the UI with the current worker status
     */
    private void syncStatus() {

        int drawableId;
        String statusText;
        String buttonText;
        if (isWorkerOn()) {
            drawableId = R.drawable.ic_on;
            statusText = "The gateway is ON";
            buttonText = "Stop gateway";
        } else {
            drawableId = R.drawable.ic_off;
            statusText = "The gateway is OFF";
            buttonText = "Start gateway";
        }

        ImageView onoffImage = findViewById(R.id.onoff_image);
        if (onoffImage != null) {
            onoffImage.setImageDrawable(getResources().getDrawable(drawableId, getApplicationContext().getTheme()));
        }

        TextView onoffStatus = findViewById(R.id.onoff_status);
        if (onoffStatus != null) {
            onoffStatus.setText(statusText);
        }

        Button startStopButton = findViewById(R.id.bStartStop);
        if (startStopButton != null) {
            startStopButton.setText(buttonText);
        }

        TextView numSmsView = findViewById(R.id.num_sms);
        if (numSmsView != null) {
            numSmsView.setText("" + getPrefsService().getInt(R.string.pref_numsms));
        }

        TextView lastSmsView = findViewById(R.id.last_sms_timestamp);
        if (lastSmsView != null) {
            lastSmsView.setText(getPrefsService().getString(R.string.pref_date_last_sms));
        }


    }


    /**
     * observe the progress of the worker
     */
    private void observeWorker() {

        UUID workerId = getWorkerId();

        if (workerId != null) {

            WorkManager workManager = WorkManager.getInstance(getApplicationContext());

            workManager.getWorkInfoByIdLiveData(workerId)
                    .observe(MainActivity.this, new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(@Nullable WorkInfo workInfo) {
                            if (workInfo != null) {

                                // l'observer dei dati pare che non funzioni con il periodic work,
                                // su stackoverflow suggeriscono di persistere il dato
                                // https://stackoverflow.com/questions/66976439/workmanager-how-to-get-outputdata-from-a-periodic-work
//                                Data data = workInfo.getOutputData();
//                                Data progress = workInfo.getProgress();

                                // Do something with progress
                                syncStatus();

                            }
                        }
                    });


        }

    }


    /**
     * @return the list of the active workers (the size should always be 0 or 1)
     */
    List<WorkInfo> listValidWorkers() throws ExecutionException, InterruptedException {
        WorkManager wm = WorkManager.getInstance(getApplicationContext());
        ListenableFuture<List<WorkInfo>> future = wm.getWorkInfosByTag(WORK_REQUEST_TAG);
        List<WorkInfo> workInfoList = future.get();

        List<WorkInfo> activeWorkInfoList = new ArrayList<>();
        for (WorkInfo wi : workInfoList) {
            WorkInfo.State state = wi.getState();
            if (!state.equals(WorkInfo.State.CANCELLED)) {
                activeWorkInfoList.add(wi);
            }
        }

        return activeWorkInfoList;
    }


    /**
     * Toggle the worker ON/OFF status
     */
    void toggleWorker() throws ExecutionException, InterruptedException {
        if (isWorkerOn()) {

            try {
                stopWorker();
            } catch (Exception e) {
                Toast.makeText(this, "Stop failed. " + e.getMessage(), Toast.LENGTH_LONG).show();
                getLogService().logE("Gateway stop failed", e);
            }

        } else {

            try {
                startWorker();
            } catch (Exception e) {
                Toast.makeText(this, "Start failed. " + e.getMessage(), Toast.LENGTH_LONG).show();
                getLogService().logE("Gateway start failed", e);
            }

        }
    }

    private boolean isWorkerOn() {
        return getWorkerId() != null;
    }

    /**
     * @return the UUID of the active worker
     */
    private UUID getWorkerId() {

        List<WorkInfo> workers = null;
        try {
            workers = listValidWorkers();
        } catch (ExecutionException | InterruptedException e) {
            getLogService().logE(e);
            return null;
        }

        if (workers.size() > 0) {
            WorkInfo workInfo = workers.get(0);
            return workInfo.getId();
        } else {
            return null;
        }

    }


    void startWorker() {

        // reset the status info
        getPrefsService().putInt(R.string.pref_numsms, 0);
        getPrefsService().putString(R.string.pref_date_last_sms, "never");
        syncStatus();

        // prepare constraints
        Constraints.Builder builder = new Constraints.Builder();
        builder.setRequiredNetworkType(NetworkType.CONNECTED);  //Any working network connection is required for this work
        Constraints constraints = builder.build();
        // (at jan 2022, there is no constraint for cellular network present)

        // retrieve the polling interval
        String sMinutes = getPrefsService().getString(R.string.interval_minutes);
        int interval;
        try {
            interval = Integer.parseInt(sMinutes);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Invalid number '" + sMinutes + "' in polling interval");
        }

        // validate the polling interval
        if (interval < 15) {
            throw new NumberFormatException("Invalid polling interval: " + interval + ". Minimum interval is 15 minutes");
        }


        // build the periodic WorkRequest
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(GatewayWorker.class, interval, TimeUnit.MINUTES)
                        .addTag(WORK_REQUEST_TAG)
                        .setConstraints(constraints)
                        .setInitialDelay(5, TimeUnit.SECONDS)
                        .build();

        // enqueue the WorkRequest
        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);

        // observe the progress
        observeWorker();

        getLogService().logI("Worker started");

    }


    void stopWorker() {

        if (!isWorkerOn()) {
            getLogService().logI("No active workers to stop");
            return;
        }

        WorkManager wm = WorkManager.getInstance(getApplicationContext());
        wm.cancelAllWorkByTag(WORK_REQUEST_TAG);
        getLogService().logI("Worker stopped");

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_log) {
            Intent intent = new Intent(this, LogActivity.class);
            startActivity(intent);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    private LogService getLogService() {
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        return appContainer.getLogService();
    }

    public PrefsService getPrefsService() {
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        return appContainer.getPrefsService();
    }


}