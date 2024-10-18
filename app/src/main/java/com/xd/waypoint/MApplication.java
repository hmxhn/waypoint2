package com.xd.waypoint;

import android.app.Application;
import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.multidex.MultiDex;

import com.secneo.sdk.Helper;
import com.xd.waypoint.fragment.DJIDemoApplication;

public class MApplication extends Application {
    private DJIDemoApplication fpvDemoApplication;
    private static Context context;
    @Override
    protected void attachBaseContext(Context paramContext){
        super.attachBaseContext(paramContext);
        MultiDex.install(this);
        com.secneo.sdk.Helper.install(MApplication.this);
        if (fpvDemoApplication==null){
            fpvDemoApplication=new DJIDemoApplication();
            fpvDemoApplication.setContext(this);
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        fpvDemoApplication.onCreate();
        context=getApplicationContext();
    }

    public static Context getContext(){
        return context;
    }

}
