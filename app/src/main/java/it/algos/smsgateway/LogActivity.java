package it.algos.smsgateway;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import it.algos.smsgateway.databinding.FragmentLogBinding;
import it.algos.smsgateway.databinding.FragmentMainBinding;

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
            Utils.logE(e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


    public void showLog() throws IOException {
        String log = SmsGatewayApp.getLog();
        TextView tv = findViewById(R.id.log_view);
        tv.setText(log);
    }

    public void clearLog() {
        SmsGatewayApp.clearLog();
        try {
            showLog();
        } catch (IOException e) {
            Utils.logE(e);
        }
    }


//    public static class LogFragment extends Fragment {
//
//        private FragmentLogBinding binding;
//
//        public LogFragment() {
//            super(R.layout.fragment_log);
//        }
//
//        @Nullable
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//            binding = FragmentLogBinding.inflate(inflater, container, false);
//            return binding.getRoot();
//        }
//
//
//        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//            super.onViewCreated(view, savedInstanceState);
//
//            binding.bClearLog.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    SmsGatewayApp.clearLog();
////                    try {
////                        showLog();
////                    } catch (IOException e) {
////                        Utils.logE(e);
////                    }
//                }
//            });
//
//        }
//
//
//    }


}