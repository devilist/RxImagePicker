/*
 * Copyright  2017  zengpu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.errang.rximagepicker.widget.dialog;


import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

import com.errang.rximagepicker.utils.StatusbarUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * dialogFragment with custom enter and exit animation(or animator)
 * <p>
 * Created by zengp on 2017/9/5.
 */

public class BaseAnimDialog extends DialogFragment implements IAnimCreator, Runnable {

    protected int mScreenWidth, mScreenHeight;
    private long mEnterAnimDuration = 400, mExitAnimDuration = 300;

    private boolean isEnterAnimFinish = false;
    private boolean isEXitAnimFinish = false;

    protected List<OnDismissListener> mListeners;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScreenWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        mScreenHeight = getActivity().getWindowManager().getDefaultDisplay().getHeight();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            mScreenHeight -= StatusbarUtil.getStatusBarHeight(getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        initView();
    }

    protected void initView() {
    }

    public void setEnterAnimDuration(long duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("Anim duration cannot be negative");
        }
        this.mEnterAnimDuration = duration;
    }

    public void setExitAnimDuration(long duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("Anim duration cannot be negative");
        }
        this.mExitAnimDuration = duration;
    }

    public void show(String tag, FragmentManager manager) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    public void show(FragmentManager manager) {
        this.show("BaseAnimDialog", manager);
    }

    public int show(FragmentTransaction transaction) {
        return super.show(transaction, "BaseAnimDialog");
    }

    @Override
    public void doEnterAnim(View contentView, long animDuration) {
        // default implementation
        // scale alpha contentView
        ScaleAnimation scaleAnimation = new ScaleAnimation(0F, 1.0F, 0F, 1.0F,
                Animation.RELATIVE_TO_PARENT, 0.5F, Animation.RELATIVE_TO_PARENT, 0.5F);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(scaleAnimation);
        animationSet.setDuration(animDuration);
        animationSet.setFillAfter(true);
        contentView.startAnimation(animationSet);
        // dim property anim for higher version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ValueAnimator dimAnimator = ValueAnimator.ofFloat(0.2f, 0.7f);
            dimAnimator.setDuration(animDuration - 100 < 0 ? animDuration : animDuration - 100);
            dimAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float offset = (float) animation.getAnimatedValue();
                    getDialog().getWindow().setDimAmount(offset);
                }
            });
            dimAnimator.start();
        }
    }

    @Override
    public void doExitAnim(View contentView, long animDuration) {
        // default implementation
        // dim
        ValueAnimator dimAnimator = ValueAnimator.ofFloat(0.7f, 0);
        dimAnimator.setDuration(animDuration - 100 < 0 ? animDuration : animDuration - 100);
        dimAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float offset = (float) animation.getAnimatedValue();
                if (null != getDialog() && null != getDialog().getWindow())
                    getDialog().getWindow().setDimAmount(offset);
            }
        });
        dimAnimator.start();
        // scale alpha contentView
        ScaleAnimation scaleAnimation = new ScaleAnimation(1.0F, 0.0F, 1.0F, 0.0F,
                Animation.RELATIVE_TO_PARENT, 0.5F, Animation.RELATIVE_TO_PARENT, 0.5F);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setDuration(animDuration);
        animationSet.setFillAfter(true);
        animationSet.setInterpolator(new AccelerateInterpolator());
        contentView.startAnimation(animationSet);
    }

    @Override
    final public void dismiss() {
        if (!isEXitAnimFinish) {
            doExitAnim(getView(), mExitAnimDuration);
            getView().postDelayed(this, mExitAnimDuration);
            isEXitAnimFinish = true;
        }
    }

    @NonNull
    @Override
    final public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new InnerAnimDialog(getActivity(), getTheme());
    }

    @Override
    final public void run() {
        super.dismissAllowingStateLoss();
        if (null != mListeners) {
            for (BaseAnimDialog.OnDismissListener listener : mListeners)
                listener.onDismiss();
        }
    }

    private class InnerAnimDialog extends Dialog implements Runnable {

        InnerAnimDialog(@NonNull Context context, @StyleRes int themeResId) {
            super(context, themeResId);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            if (!isEnterAnimFinish) {
                doEnterAnim(getView(), mEnterAnimDuration);
                isEnterAnimFinish = true;
            }
        }

        @Override
        public void cancel() {
            if (!isCancelable())
                return;
            if (!isEXitAnimFinish) {
                doExitAnim(getView(), mExitAnimDuration);
                getView().postDelayed(this, mExitAnimDuration);
                isEXitAnimFinish = true;
            }
        }

        @Override
        public void run() {
            super.cancel();
            if (null != mListeners) {
                for (BaseAnimDialog.OnDismissListener listener : mListeners)
                    listener.onDismiss();
            }
        }
    }

    public void setOnDismissListener(OnDismissListener listener) {
        if (null == mListeners) mListeners = new ArrayList<>();
        mListeners.add(listener);
    }

    public interface OnDismissListener {
        void onDismiss();
    }
}
