package it.algos.smsgateway;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutionException;

import it.algos.smsgateway.databinding.FragmentMainBinding;
import it.algos.smsgateway.services.LogService;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;

    public MainFragment() {
        super(R.layout.fragment_main);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.bStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ((MainActivity) getActivity()).toggleWorker();
                } catch (ExecutionException | InterruptedException e) {
                    getLogService().logE(e);
                }
            }
        });

//        binding.button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    ((MainActivity)getActivity()).showLog();
//                } catch (IOException e) {
//                    Utils.logException(e);
//                }
//            }
//        });


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    private LogService getLogService() {
        AppContainer appContainer = ((SmsGatewayApp) getActivity().getApplicationContext()).appContainer;
        return appContainer.getLogService();
    }


}