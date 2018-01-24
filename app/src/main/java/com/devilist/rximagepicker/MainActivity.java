package com.devilist.rximagepicker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by zengp on 2018/1/24.
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectImageActivity.start(MainActivity.this);
            }
        });
        findViewById(R.id.tv_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectedVideoActivity.start(MainActivity.this);
            }
        });
    }
}
