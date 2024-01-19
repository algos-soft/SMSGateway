package it.algos.smsgateway.testsms;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import it.algos.smsgateway.AppContainer;
import it.algos.smsgateway.R;
import it.algos.smsgateway.SmsGatewayApp;
import it.algos.smsgateway.logging.LogFragment;

public class TestSmsActivity extends AppCompatActivity {

    public TestSmsActivity() {
        super(R.layout.testsms_activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppContainer appContainer = ((SmsGatewayApp) getApplicationContext()).appContainer;
//        logService =appContainer.logService;

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.log_fragment_container_view, TestSmsFragment.class, null)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}