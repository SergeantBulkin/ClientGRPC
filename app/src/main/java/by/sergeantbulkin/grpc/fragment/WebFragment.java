package by.sergeantbulkin.grpc.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import by.sergeantbulkin.grpc.databinding.FragmentWebBinding;
import by.sergeantbulkin.grpc.model.MyWebViewClient;

public class WebFragment extends Fragment
{
    //----------------------------------------------------------------------------------------------
    private FragmentWebBinding binding;
    //----------------------------------------------------------------------------------------------
    public WebFragment()
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        binding = FragmentWebBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    //----------------------------------------------------------------------------------------------

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        //Настройка
        setUpViews();
    }

    private void setUpViews()
    {
        binding.buttonNext.setOnClickListener(v ->
        {
            if (binding.webView.canGoBack())
            {
                binding.webView.goBack();
            }
        });

        binding.webView.setWebViewClient(new MyWebViewClient(binding.headerTextView));
        binding.webView.getSettings().setJavaScriptEnabled(true);

        binding.webView.loadUrl("https://accounts.google.com/signin/v2/identifier?hl=ru&flowName=GlifWebSignIn&flowEntry=ServiceLogin");
    }
    //----------------------------------------------------------------------------------------------
}