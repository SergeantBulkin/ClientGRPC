package by.sergeantbulkin.grpc.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import by.sergeantbulkin.grpc.R;
import by.sergeantbulkin.grpc.databinding.FragmentBaseBinding;
import by.sergeantbulkin.proto.MethodRequest;
import by.sergeantbulkin.proto.MethodResponse;
import by.sergeantbulkin.proto.methodGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class BaseFragment extends Fragment
{
    //----------------------------------------------------------------------------------------------
    private final String HOST = "192.168.43.231";
    private final String PORT = "9090";
    //----------------------------------------------------------------------------------------------
    FragmentBaseBinding binding;
    //----------------------------------------------------------------------------------------------
    public BaseFragment()
    {
        // Required empty public constructor
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        binding = FragmentBaseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        //Установка слушателя
        setUpViews();
    }
    //----------------------------------------------------------------------------------------------
    private void setUpViews()
    {
        binding.button.setOnClickListener(v ->
        {
            sendMessage();
        });
    }
    //----------------------------------------------------------------------------------------------
    private void sendMessage()
    {
        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(binding.textView.getWindowToken(), 0);
        binding.button.setEnabled(false);
        binding.textView.setText("");

        new GrpcTask(requireActivity())
                .execute(HOST, PORT);
    }
    //----------------------------------------------------------------------------------------------
    private static class GrpcTask extends AsyncTask<String, Void, String>
    {
        private final WeakReference<Activity> activityWeakReference;
        private ManagedChannel channel;

        private GrpcTask(Activity activity)
        {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... strings)
        {
            String host = strings[0];
            int port = Integer.parseInt(strings[1]);

            try
            {
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
                methodGrpc.methodBlockingStub stub = methodGrpc.newBlockingStub(channel);
                MethodRequest request = MethodRequest.newBuilder().build();
                MethodResponse response = stub.getMethod(request);
                return String.valueOf(response.getResponseCode());
            } catch (Exception e)
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return String.format("Failed... : %n%s", sw);
            }
        }

        @Override
        protected void onPostExecute(String s)
        {
            try
            {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            Activity activity = activityWeakReference.get();
            if (activity == null)
            {
                return;
            }
            TextView result = (TextView) activity.findViewById(R.id.textView);
            Button button = (Button) activity.findViewById(R.id.button);
            result.setText(s);
            button.setEnabled(true);
        }
    }
    //----------------------------------------------------------------------------------------------
}