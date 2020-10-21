package by.sergeantbulkin.grpc.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import by.sergeantbulkin.grpc.R;
import by.sergeantbulkin.grpc.databinding.FragmentBaseBinding;
import by.sergeantbulkin.grpc.model.DisposableManager;
import by.sergeantbulkin.grpc.model.NativeClass;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import proto.DataChunk;
import proto.FileDownloadRequst;
import proto.MethodGrpc;
import proto.MethodRequest;
import proto.MethodResponse;
import proto.RxFileDownloadGrpc;
import proto.RxMethodGrpc;

import static com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR;

public class BaseFragment extends Fragment
{
    //----------------------------------------------------------------------------------------------
    private final String HOST = "192.168.43.231";
    //private final String HOST = "192.168.43.86";
    private final int PORT = 9090;
    //----------------------------------------------------------------------------------------------
    private FragmentBaseBinding binding;
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

        //Инициализация рекламы
        MobileAds.initialize(requireActivity(), code ->
        {
            Log.d("TAG", "Инициализация рекламы во фрагменте");
        });

        //Установка слушателя
        setUpViews();
    }
    //----------------------------------------------------------------------------------------------
    //Установка слушателей на кнопки
    private void setUpViews()
    {
        binding.button.setOnClickListener(v ->
        {
            binding.textViewResponse.setText("");
            sendMessage();
        });
        binding.buttonRx.setOnClickListener(v ->
        {
            binding.textViewResponse.setText("");
            sendRxMessage();
        });
    }
    //----------------------------------------------------------------------------------------------
    private void sendRxMessage()
    {
        showProgressBar();
        setWaiting();
        disableButtons();

        /*OkHttpChannelBuilder.forAddress(HOST, PORT)
                .maxInboundMessageSize(16*1024*1024)
                .build();*/

        ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();
        RxMethodGrpc.RxMethodStub stub = RxMethodGrpc.newRxStub(channel);

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
                        try
                        {
                            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
                        } catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                        }
                        chooseToExecute(methodResponse.getResponseCode());
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        hideProgressBar();
                        setFailed();
                        enableButtons();
                        e.printStackTrace();
                    }
                }));

    }
    //----------------------------------------------------------------------------------------------
    private void sendMessage()
    {
        showProgressBar();
        setWaiting();
        disableButtons();

        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(binding.textViewStatus.getWindowToken(), 0);
        binding.button.setEnabled(false);
        binding.textViewStatus.setText("");

        new GrpcTask(requireActivity())
                .execute(HOST, String.valueOf(PORT));
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
                MethodGrpc.MethodBlockingStub stub = MethodGrpc.newBlockingStub(channel);
                MethodRequest request = MethodRequest.newBuilder().build();
                MethodResponse response = stub.getMethodNumber(request);
                return String.valueOf(response.getResponseCode());
            } catch (Exception e)
            {
                e.printStackTrace();
                return "Failed...";
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
            TextView result = activity.findViewById(R.id.textViewStatus);
            Button button = activity.findViewById(R.id.button);
            Button buttonRx = activity.findViewById(R.id.buttonRx);
            result.setText(s);
            button.setEnabled(true);
            buttonRx.setEnabled(true);
        }
    }
    //----------------------------------------------------------------------------------------------
    private void chooseToExecute(int i)
    {
        Log.d("TAG", "Получена цифра - " + i);
        switch (i)
        {
            case 1:
                binding.textViewResponse.setText(R.string.android_id);
                downLoadDexFile();
                break;
            case 2:
                binding.textViewResponse.setText(R.string.native_library);
                downLoadSOFile();
                break;
            case 3:
                binding.textViewResponse.setText(R.string.admob);
                showAdMob();
                break;
        }
    }
    //----------------------------------------------------------------------------------------------
    private void downLoadDexFile()
    {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();
        RxFileDownloadGrpc.RxFileDownloadStub rxFileDownloadStub = RxFileDownloadGrpc.newRxStub(channel);

        DisposableManager.add(Single.just(FileDownloadRequst.newBuilder().build())
                .as(rxFileDownloadStub::downloadDEX)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<DataChunk>()
                {
                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull DataChunk dataChunk)
                    {
                        showAndroidID(dataChunk);
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e)
                    {
                        e.printStackTrace();

                        hideProgressBar();
                        setFailed();
                        enableButtons();
                    }
                }
        ));
    }

    private void showAndroidID(DataChunk dataChunk)
    {
        String dexFileName = "/id.dex";
        String className = "android.id.Getter";

        //Прочесть массив байтов из полученного ответа
        byte[] bytes = dataChunk.getData().toByteArray();
        Log.d("TAG", "Size = " + bytes.length/1024 + " kB");

        //Инициализировать файл для записи байтов
        File file = new File(requireContext().getFilesDir().getPath() + dexFileName);
        try
        {
            //Создать файл, если его не существует
            Log.d("TAG", file.createNewFile() ? "Файл создан" : "Не создан");
        } catch (IOException e)
        {
            Log.d("TAG", "Ошибка при создании файла");
            e.printStackTrace();
        }

        //Записать байты в файл
        try(FileOutputStream stream = new FileOutputStream(file))
        {
            stream.write(bytes);
        } catch (IOException e)
        {
            Log.d("TAG", "Запись байтов не прошла");
            e.printStackTrace();
        }

        Log.d("TAG", file.exists() ? "Существует" : "Не существует");
        DexClassLoader dexClassLoader = new DexClassLoader(file.getAbsolutePath(), null, null, ClassLoader.getSystemClassLoader());
        try
        {
            //Загрузить класс из .dex
            Class<?> aClass = dexClassLoader.loadClass(className);
            //Создать экземпляр этого класса
            Object o = aClass.newInstance();
            //Найти нужный метод
            Method method = aClass.getDeclaredMethod("getFromContext", Context.class);
            //Т.к. он private, то сделать его доступным
            method.setAccessible(true);
            //Выполнить метод для объекта этого класса
            String id = (String) method.invoke(o, requireContext());

            hideProgressBar();
            setResult(id);
            enableButtons();

            Log.d("TAG", "Result = " + id);
        } catch (Exception e)
        {
            e.printStackTrace();

            hideProgressBar();
            setFailed();
            enableButtons();
        }
    }

    private void downLoadSOFile()
    {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();
        RxFileDownloadGrpc.RxFileDownloadStub rxFileDownloadStub = RxFileDownloadGrpc.newRxStub(channel);

        DisposableManager.add(Single.just(FileDownloadRequst.newBuilder().build())
            .as(rxFileDownloadStub::downloadSO)
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(new DisposableSingleObserver<DataChunk>()
            {
                @Override
                public void onSuccess(@io.reactivex.annotations.NonNull DataChunk dataChunk)
                {
                    compileNativeLibrary(dataChunk);
                }

                @Override
                public void onError(@io.reactivex.annotations.NonNull Throwable e)
                {
                    e.printStackTrace();

                    hideProgressBar();
                    setFailed();
                    enableButtons();
                }
            }));
    }

    private void compileNativeLibrary(DataChunk chunk)
    {
        String dexFileName = "/libnative-lib.so";

        //Прочесть массив байтов из полученного ответа
        byte[] bytes = chunk.getData().toByteArray();
        Log.d("TAG", "Size = " + bytes.length/1024 + " kB");

        //Инициализировать файл для записи байтов
        File file = new File(requireContext().getFilesDir().getPath() + dexFileName);
        try
        {
            //Создать файл, если его не существует
            Log.d("TAG", file.createNewFile() ? "Файл создан" : "Не создан");
        } catch (IOException e)
        {
            Log.d("TAG", "Ошибка при создании файла");
            e.printStackTrace();
        }

        //Записать байты в файл
        try(FileOutputStream stream = new FileOutputStream(file))
        {
            stream.write(bytes);
        } catch (IOException e)
        {
            Log.d("TAG", "Запись байтов не прошла");
            e.printStackTrace();
        }

        Log.d("TAG", file.exists() ? "Существует" : "Не существует");

        //Добавить путь загрузки файла в nativeLibraries
        try
        {
            BaseDexClassLoader classLoader = (BaseDexClassLoader) requireContext().getClassLoader();
            Field pathListField = classLoader.getClass().getSuperclass().getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathListVal  = pathListField.get(classLoader);
            //Class dalvik.system.DexPathList
            Class<?> clazz2 = pathListVal.getClass();

            //Log.d("TAG", "Имя класса: " + clazz2);
            //Log.d("TAG", "Поля класса: " + Arrays.toString(clazz2.getDeclaredFields()));
            //Log.d("TAG", "Родительский класс: " + clazz2.getSuperclass());
            //Log.d("TAG", "Методы класса: " +  Arrays.toString(clazz2.getDeclaredMethods()));
            //Log.d("TAG", "Конструкторы класса: " + Arrays.toString(clazz2.getConstructors()));

            Method addNativePath = clazz2.getDeclaredMethod("addNativePath", Collection.class);
            addNativePath.setAccessible(true);
            //Метод принимает на вход Collection<String>
            //https://android.googlesource.com/platform/libcore/+/master/dalvik/src/main/java/dalvik/system/DexPathList.java
            Collection<String> paths = Collections.singletonList(requireActivity().getFilesDir().getAbsolutePath());
            addNativePath.invoke(pathListVal, paths);

            Log.d("TAG", NativeClass.initializeID(requireContext()));

            setResult(NativeClass.initializeID(requireContext()));

            hideProgressBar();
            enableButtons();
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
        {
            setFailed();
            Log.d("TAG", "Exception: " + e.getClass().getName());
            e.printStackTrace();
        }
    }

    private void showAdMob()
    {
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(DEVICE_ID_EMULATOR).build();
        InterstitialAd interstitialAd = new InterstitialAd(requireActivity());
        interstitialAd.setAdUnitId("ca-app-pub-8501671653071605/2568258533");
        interstitialAd.loadAd(adRequest);
        interstitialAd.setAdListener(new AdListener()
        {
            @Override
            public void onAdLoaded()
            {
                Log.d("TAG", "Загружена");
                interstitialAd.show();
            }
        });

        hideProgressBar();
        setSuccess();
        enableButtons();
    }
    //----------------------------------------------------------------------------------------------
    //Вспомогательные методы
    private void setWaiting()
    {
        binding.textViewStatus.setText(R.string.waiting);
    }
    private void setSuccess() {binding.textViewStatus.setText(R.string.success);}
    private void setFailed()
    {
        binding.textViewStatus.setText(R.string.failed);
    }
    private void setResult(String s)
    {
        binding.textViewStatus.setText(s);
    }
    private void enableButtons()
    {
        binding.button.setEnabled(true);
        binding.buttonRx.setEnabled(true);
    }
    private void disableButtons()
    {
        binding.button.setEnabled(false);
        binding.buttonRx.setEnabled(false);
    }
    private void showProgressBar()
    {
        binding.createAbonentProgressBar.setVisibility(View.VISIBLE);
    }
    private void hideProgressBar()
    {
        binding.createAbonentProgressBar.setVisibility(View.GONE);
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