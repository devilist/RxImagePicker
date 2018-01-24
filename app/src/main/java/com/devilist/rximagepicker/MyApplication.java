package com.devilist.rximagepicker;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.errang.rximagepicker.RxImagePicker;


/**
 * Created by zengp on 2017/7/9.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RxImagePicker.init(new GlideImageLoader(), getAppExternalStorageDirRoot(this));
    }

    private String getAppExternalStorageDirRoot(Context context) {
        String externalPath = Environment.getExternalStorageDirectory().getPath();
        return externalPath + "/" + context.getPackageName() + "/";
    }
}
