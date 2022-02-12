package it.algos.smsgateway.logging;

import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import it.algos.smsgateway.AppContainer;
import it.algos.smsgateway.R;
import it.algos.smsgateway.SmsGatewayApp;
import it.algos.smsgateway.services.LogService;

public class LogActivity extends AppCompatActivity {

//    private LogService logService;


    public LogActivity() {
        super(R.layout.activity_log);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
//        logService =appContainer.logService;

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.log_fragment_container_view, LogFragment.class, null)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        try {
            showLog();
        } catch (IOException e) {
            getLogService().logE(e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


    public void showLog() throws IOException {
        String log = getLogService().getLog();
        TextView tv = findViewById(R.id.log_view);
        tv.setText(log);
    }

    public void clearLog() {
        getLogService().clearLog();
        try {
            showLog();
        } catch (IOException e) {
            getLogService().logE(e);
        }
    }

    public void exportLog() {

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String filename = "smsgateway_" + timestamp + ".log";
        File file = new File(downloadsDir, filename);

        try {

            if (file.exists()) {
                file.delete();
            }

            // write the log to the file
            FileOutputStream fOut = new FileOutputStream(file, true);
            String string = getLogService().getLog();
            byte[] data = (string.getBytes());
            fOut.write(data);
            fOut.close();
            Toast.makeText(getApplicationContext(),
                    filename + " saved in Downloads",
                    Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e) {
            getLogService().logE(e);
        } catch (IOException e) {
            getLogService().logE(e);
        }

    }

    public LogService getLogService() {
        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
        return appContainer.getLogService();
    }


}