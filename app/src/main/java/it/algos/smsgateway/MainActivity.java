package it.algos.smsgateway;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.util.Log;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import it.algos.smsgateway.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String WORK_REQUEST_TAG="it.algos.smsgateway.WORK_REQUEST";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


        checkPermissions();

        syncStatus();

        observeWorker();

    }


    private void checkPermissions(){

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            String[] permissionsArray=new String[]{Manifest.permission.SEND_SMS};
            requestPermissions(permissionsArray, 3);
        }

    }


    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==3){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Showing the toast message
                Toast.makeText(MainActivity.this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(MainActivity.this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();

                finishAndRemoveTask();

            }

        }

    }





    /**
     * Update the UI with the current worker status
     */
    private void syncStatus()  {

        int drawableId;
        String statusText;
        String buttonText;
        if(isWorkerOn()){
            drawableId = R.drawable.ic_on;
            statusText="The gateway is ON";
            buttonText="Stop gateway";
        }else{
            drawableId = R.drawable.ic_off;
            statusText="The gateway is OFF";
            buttonText="Start gateway";
        }

        ImageView onoffImage = findViewById(R.id.onoff_image);
        if(onoffImage!=null){
            onoffImage.setImageDrawable(getResources().getDrawable(drawableId, getApplicationContext().getTheme()));
        }

        TextView onoffStatus = findViewById(R.id.onoff_status);
        if(onoffStatus!=null){
            onoffStatus.setText(statusText);
        }

        Button startStopButton = findViewById(R.id.bStartStop);
        if(startStopButton!=null){
            startStopButton.setText(buttonText);
        }

        TextView numSmsView = findViewById(R.id.num_sms);
        if(numSmsView!=null){
            numSmsView.setText(""+ Prefs.getInt(getApplicationContext(), R.string.pref_numsms));
        }

        TextView lastSmsView = findViewById(R.id.last_sms_timestamp);
        if(lastSmsView!=null){
            lastSmsView.setText(Prefs.getString(getApplicationContext(), R.string.pref_date_last_sms));
        }





    }


    /**
     * observe the progress of the worker
     */
    private void observeWorker(){

        UUID workerId = getWorkerId();

        if(workerId!=null){

            WorkManager workManager= WorkManager.getInstance(getApplicationContext());

            workManager.getWorkInfoByIdLiveData(workerId)
                    .observe(MainActivity.this, new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(@Nullable WorkInfo workInfo) {
                            if (workInfo != null) {

                                // l'observer dei dati pare che non funzioni con il periodic work,
                                // su stackoverflow suggeriscono di persistere il dato
                                // https://stackoverflow.com/questions/66976439/workmanager-how-to-get-outputdata-from-a-periodic-work
                                Data data = workInfo.getOutputData();
                                Data progress = workInfo.getProgress();

//                                numSmsSent = Prefs.getInt(getApplicationContext(), R.string.pref_numsms);
//                                numSmsSent = progress.getInt(Constants.NUM_SMS_SENT_KEY, 0);

                                // Do something with progress
                                syncStatus();
                                //syncNumSmsSent();

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
        for(WorkInfo wi : workInfoList){
            WorkInfo.State state = wi.getState();
            if(!state.equals(WorkInfo.State.CANCELLED)){
                activeWorkInfoList.add(wi);
            }
        }

        return activeWorkInfoList;
    }


    /**
     * Toggle the worker ON/OFF status
     */
    void toggleWorker() throws ExecutionException, InterruptedException {
        if(isWorkerOn()){
            stopWorker();
        }else {
            startWorker();
        }
    }

    private boolean isWorkerOn(){
        return getWorkerId()!=null;
    }

    /**
     * @return the UUID of the active worker
     */
    private UUID getWorkerId(){

        List<WorkInfo> workers = null;
        try {
            workers = listValidWorkers();
        } catch (ExecutionException | InterruptedException e) {
            Utils.logException(e);
            return null;
        }

        if(workers.size()>0){
            WorkInfo workInfo = workers.get(0);
            return workInfo.getId();
        }else{
            return null;
        }

    }


    void startWorker() {

        // reset the status info
        Prefs.putInt(getApplicationContext(), R.string.pref_numsms, 0);
        Prefs.putString(getApplicationContext(), R.string.pref_date_last_sms, "never");
        syncStatus();

        // prepare constraints
        Constraints.Builder builder = new Constraints.Builder();
        builder.setRequiredNetworkType(NetworkType.CONNECTED);  //Any working network connection is required for this work
        Constraints constraints = builder.build();
        // (at jan 2022, there is no constraint for cellular network present)

        // build the periodic WorkRequest
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(QuerySendAndConfirmWorker.class, 15, TimeUnit.MINUTES)
                        .addTag(WORK_REQUEST_TAG)
                        .setConstraints(constraints)
                        .setInitialDelay(5, TimeUnit.SECONDS)
                        .build();

        // enqueue the WorkRequest
        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);

        // observe the progress
        observeWorker();

        Log.i(Constants.LOG_TAG, "Worker started");

    }


    void stopWorker() {

        if(!isWorkerOn()){
            Log.i(Constants.LOG_TAG, "No active workers to stop");
            return;
        }

        WorkManager wm = WorkManager.getInstance(getApplicationContext());
        wm.cancelAllWorkByTag(WORK_REQUEST_TAG);
        Log.i(Constants.LOG_TAG, "Worker stopped");

    }



    void showLog() throws IOException {

//        String myStringArray[]= {"logcat"};
//        Process process = Runtime.getRuntime().exec(myStringArray);
//
//        BufferedReader bufferedReader = new BufferedReader(
//                new InputStreamReader(process.getInputStream()));
//
//        // Grab the results
//        StringBuilder log = new StringBuilder();
//        String line;
//        while ((line = bufferedReader.readLine()) != null) {
//            log.append(line + "\n");
//        }


//        val logCatViewModel by viewModels<LogCatViewModel>()
//
//        logCatViewModel.logCatOutput().observe(this, Observer{ logMessage ->
//                logMessageTextView.append("$logMessage\n")
//        })
//
//        int a = 87;
//        int b=a;



        // Update the view
//        TextView tv = (TextView)findViewById(R.id.my_text_view);
//        tv.setText(log.toString());

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}