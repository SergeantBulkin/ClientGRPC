package by.sergeantbulkin.grpc.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import by.sergeantbulkin.grpc.R;
import by.sergeantbulkin.grpc.databinding.FragmentBaseBinding;
import by.sergeantbulkin.grpc.model.DisposableManager;
import by.sergeantbulkin.proto.MethodRequest;
import by.sergeantbulkin.proto.MethodResponse;
import by.sergeantbulkin.proto.RxmethodGrpc;
import by.sergeantbulkin.proto.methodGrpc;
import dalvik.system.DexFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class BaseFragment extends Fragment
{
    //----------------------------------------------------------------------------------------------
    private final String HOST = "192.168.43.231";
    private final int PORT = 9090;
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
            clearText();
            sendMessage();
        });
        binding.buttonRx.setOnClickListener(v ->
        {
            clearText();
            sendRxMessage();
        });
    }
    //----------------------------------------------------------------------------------------------
    private void sendRxMessage()
    {
        binding.buttonRx.setEnabled(false);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();
        RxmethodGrpc.RxmethodStub stub = RxmethodGrpc.newRxStub(channel);

        DisposableManager.add(Single
                .just(MethodRequest.newBuilder().build())
                .as(stub::getMethodNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<MethodResponse>()
                {
                    @Override
                    public void onSuccess(MethodResponse methodResponse)
                    {
                        binding.textView.setText(String.valueOf(methodResponse.getResponseCode()));
                        binding.buttonRx.setEnabled(true);
                        try
                        {
                            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
                        } catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        e.printStackTrace();
                    }
                }));

    }
    //----------------------------------------------------------------------------------------------
    private void sendMessage()
    {
        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(binding.textView.getWindowToken(), 0);
        binding.button.setEnabled(false);
        binding.textView.setText("");

        new GrpcTask(requireActivity())
                .execute(HOST, String.valueOf(PORT));
    }
    //----------------------------------------------------------------------------------------------
    private void clearText()
    {
        binding.textView.setText("");
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
                MethodResponse response = stub.getMethodNumber(request);
                return String.valueOf(response.getResponseCode());
            } catch (Exception e)
            {
                e.printStackTrace();
                return "Failed... ";
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
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        DisposableManager.dispose();
    }
    //----------------------------------------------------------------------------------------------
}