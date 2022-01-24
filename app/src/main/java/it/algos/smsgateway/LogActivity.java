package it.algos.smsgateway;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogActivity extends AppCompatActivity {


    public LogActivity() {
        super(R.layout.activity_log);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            Utils.logException(e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


    void showLog() throws IOException {

//        String myStringArray[]= {"logcat -d"};
        Process process = Runtime.getRuntime().exec("logcat -d SMSGateway:I *:S");

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        // Grab the results
        StringBuilder log = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            log.append(line + "\n");
        }

        int a = 87;
        int b=a;

        TextView tv = (TextView)findViewById(R.id.log_view);
        tv.setText(log.toString());



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


    public static class LogFragment extends Fragment {

        public LogFragment() {
            super(R.layout.fragment_log);
        }

    }


}