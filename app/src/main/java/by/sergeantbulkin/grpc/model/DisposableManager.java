package by.sergeantbulkin.grpc.model;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class DisposableManager
{
    //----------------------------------------------------------------------------------------------
    //Singleton объект
    private static CompositeDisposable compositeDisposable;
    //----------------------------------------------------------------------------------------------
    //Конструктор
    private DisposableManager() {}
    //----------------------------------------------------------------------------------------------
    //Создаст объект только, если он еще не существует
    private static CompositeDisposable getCompositeDisposable()
    {
        if (compositeDisposable == null || compositeDisposable.isDisposed())
        {
            compositeDisposable = new CompositeDisposable();
        }
        return compositeDisposable;
    }
    //----------------------------------------------------------------------------------------------
    public static void add(Disposable disposable)
    {
        getCompositeDisposable().add(disposable);
    }
    //----------------------------------------------------------------------------------------------
    public static void dispose()
    {
        getCompositeDisposable().dispose();
    }
    //----------------------------------------------------------------------------------------------
}
