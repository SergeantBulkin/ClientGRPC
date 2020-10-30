package by.sergeantbulkin.grpc;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import by.sergeantbulkin.grpc.fragment.BaseFragment;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment, new BaseFragment()).commit();
    }
}