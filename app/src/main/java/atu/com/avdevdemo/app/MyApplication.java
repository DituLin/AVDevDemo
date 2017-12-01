package atu.com.avdevdemo.app;

import android.app.Application;
import android.content.Context;

/**
 * Created by atu on 2017/10/27.
 */

public class MyApplication extends Application {


    private static Context context;

    public static Context getInstant() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }
}
