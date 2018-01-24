package com.devilist.rximagepicker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.errang.rximagepicker.RxImagePicker;
import com.errang.rximagepicker.model.Image;

import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class SelectImageActivity extends AppCompatActivity implements View.OnClickListener {

    public static void start(Context context) {
        Intent starter = new Intent(context, SelectImageActivity.class);
        context.startActivity(starter);
    }

    private SwitchCompat sc_single, sc_crop, sc_preview, sc_camera;
    private EditText et_num;
    private TextView tv_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        sc_single = (SwitchCompat) findViewById(R.id.sc_single);
        sc_crop = (SwitchCompat) findViewById(R.id.sc_crop);
        sc_preview = (SwitchCompat) findViewById(R.id.sc_preview);
        sc_camera = (SwitchCompat) findViewById(R.id.sc_camera);
        et_num = (EditText) findViewById(R.id.et_num);
        tv_result = (TextView) findViewById(R.id.tv_result);

        findViewById(R.id.btn_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_button:
                RxImagePicker
                        .ready()
                        .limit(Integer.valueOf(et_num.getText().toString()))
                        .single(sc_single.isChecked())
                        .crop(sc_crop.isChecked())
                        .preview(sc_preview.isChecked())
                        .camera(sc_camera.isChecked())
                        .go(this)
                        .subscribe(new Consumer<List<Image>>() {
                            @Override
                            public void accept(@NonNull List<Image> images) throws Exception {
                                String result = "selected images count " + images.size();
                                for (int i = 0; i < images.size(); i++) {
                                    result += ("\n" + (i + 1) + " path :\n" + images.get(i).path);

                                    Log.d("SelectImageActivity", "name " + images.get(i).name +
                                            "  width " + images.get(i).width +
                                            "  height " + images.get(i).height +
                                            "  addTime " + images.get(i).addTime +
                                            "\npath " + images.get(i).path);
                                }
                                tv_result.setText(result);
                            }
                        });
                break;
        }

    }

    public boolean isPerMissionGranted(Context context, String permissionName) {
        PackageManager pm = context.getPackageManager();
        return PackageManager.PERMISSION_GRANTED == pm.checkPermission(permissionName, context.getPackageName());
    }
}
