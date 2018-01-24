package com.errang.rximagepicker.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.errang.rximagepicker.R;
import com.errang.rximagepicker.utils.StatusbarUtil;

/**
 * Created by zengpu on 2017/5/31.
 */

public class ImageBaseActivity extends AppCompatActivity {

    protected boolean isDestroyed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusbarUtil.setStatusBarDarkMode(this, 3);
    }

    public <T extends View> T findView(@IdRes int resId) {
        return (T) (super.findViewById(resId));
    }


    protected void initToolBar(String title) {
        // 初始化statusbar
        initStatusBar(R.id.iv_statusbar);
        TextView titleTV = findView(R.id.tv_title);
        titleTV.setText(title);
        findView(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    /**
     * 初始化StatusBar
     */
    protected void initStatusBar(@IdRes int viewId) {
        // iv_back在statusbar下面
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && viewId > 0) {
            int height = StatusbarUtil.getStatusBarHeight(this);
            findView(viewId).getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            findView(viewId).getLayoutParams().height = height;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
    }
}
