package com.gyf.immersionbar.sample;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.gyf.immersionbar.sample.service.NetworkService;
import com.tencent.bugly.crashreport.CrashReport;

import me.yokeyword.fragmentation.Fragmentation;

/**
 * @author geyifeng
 * @date 2017/8/4
 */
public class MyApp extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        // bugly
        CrashReport.initCrashReport(getApplicationContext(), "31a5f1f394", false);
        // Network listener service
        NetworkService.enqueueWork(this);
        Fragmentation.builder()
                // Display floating bubble; Other Modes: SHAKE: shake to show, NONE: hide
                .stackViewMode(Fragmentation.BUBBLE)
                .debug(BuildConfig.DEBUG)
                .install();
    }

    public static Context getContext() {
        return mContext;
    }
}
