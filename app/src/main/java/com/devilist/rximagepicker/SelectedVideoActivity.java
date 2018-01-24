package com.devilist.rximagepicker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.errang.rximagepicker.RxImagePicker;
import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.model.Video;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Created by zengp on 2018/1/24.
 */

public class SelectedVideoActivity extends AppCompatActivity implements View.OnClickListener {

    public static void start(Context context) {
        Intent starter = new Intent(context, SelectedVideoActivity.class);
        context.startActivity(starter);
    }

    private SwitchCompat sc_single;
    private EditText et_num;
    private TextView tv_result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        sc_single = (SwitchCompat) findViewById(R.id.sc_single);
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
                        .goVideo(this)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<Video>>() {
                            @Override
                            public void accept(@NonNull List<Video> videos) throws Exception {
                                String result = "selected videos count " + videos.size();
                                for (int i = 0; i < videos.size(); i++) {
                                    result += ("\n" + (i + 1) + " path :\n" + videos.get(i).path);
                                    Log.d("SelectedVideoActivity", "name " + videos.get(i).name +
                                            "  width " + videos.get(i).width +
                                            "  height " + videos.get(i).height +
                                            "  addTime " + videos.get(i).addTime +
                                            "\npath " + videos.get(i).path);
                                }
                                tv_result.setText(result);
                            }
                        });
                break;
        }
    }
}
