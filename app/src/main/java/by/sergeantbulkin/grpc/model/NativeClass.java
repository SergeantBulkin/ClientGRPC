package by.sergeantbulkin.grpc.model;

import android.content.Context;

public class NativeClass
{
    static
    {
        System.loadLibrary("native-lib");
    }

    public native static String initializeID(Context ctx);
}
