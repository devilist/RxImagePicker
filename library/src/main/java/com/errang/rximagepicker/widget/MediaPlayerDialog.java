package com.errang.rximagepicker.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.errang.rximagepicker.R;
import com.errang.rximagepicker.widget.dialog.BaseAnimDialog;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by zengp on 2017/12/17.
 */

@SuppressLint("ValidFragment")
public class MediaPlayerDialog extends BaseAnimDialog implements
        View.OnClickListener,
        BaseAnimDialog.OnDismissListener,
        TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnErrorListener,
        SeekBar.OnSeekBarChangeListener {

    public static final String TAG = "MediaPlayerDialog";

    private MediaPlayer mediaPlayer;
    private TextureView textureView;
    private AppCompatSeekBar sb_bar;
    private TextView tv_control;
    private LinearLayout controller;
    private ProgressView pv_progress;
    private RelativeLayout rl_video;
    private ImageView iv_cover;

    private String videoUrl, coverUrl;
    private boolean isHttpVideo;

    private int currentProgress = 0;

    private Timer mTimer;
    private TimerTask mTimerTask;
    private MyHandler mHandler;

    private int videoWidth, videoHeight;

    private boolean isPrepared = false;

    private class MyHandler extends Handler {
        private WeakReference<MediaPlayerDialog> parent;

        MyHandler(MediaPlayerDialog parent) {
            this.parent = new WeakReference<>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaPlayerDialog dialog = parent.get();
            if (null != dialog && msg.what == 0)
                dialog.updateSeekBar();
        }
    }

    public static MediaPlayerDialog newInstance(@NonNull String videoUrl, String coverUrl) {
        Bundle args = new Bundle();
        MediaPlayerDialog fragment = new MediaPlayerDialog(videoUrl, coverUrl);
        fragment.setArguments(args);
        return fragment;
    }

    private MediaPlayerDialog(@NonNull String videoUrl, String coverUrl) {
        this.videoUrl = videoUrl;
        this.coverUrl = coverUrl;
        isHttpVideo = null != videoUrl && videoUrl.startsWith("http");
        setEnterAnimDuration(800);
        setExitAnimDuration(500);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getDialog().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View contentView = inflater.inflate(R.layout.rx_dialog_media_player, container, false);
        return contentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(mScreenWidth, mScreenHeight);
        videoWidth = videoHeight = mScreenWidth;
    }

    @Override
    protected void initView() {
        pv_progress = getView().findViewById(R.id.pv_progress);
        sb_bar = getView().findViewById(R.id.sb_bar);
        tv_control = getView().findViewById(R.id.tv_control);
        controller = getView().findViewById(R.id.controller);
        textureView = getView().findViewById(R.id.textureview);

        iv_cover = getView().findViewById(R.id.iv_cover);
        rl_video = getView().findViewById(R.id.rl_video);
        rl_video.getLayoutParams().width = mScreenWidth;
        rl_video.getLayoutParams().height = mScreenWidth;
        sb_bar.setEnabled(false);
        tv_control.setActivated(false);
        if (isHttpVideo) pv_progress.setCurrentProgress(0.5f);
//        Glide.with(getContext()).load(coverUrl).asBitmap().into(
//                new BitmapImageViewTarget(iv_cover) {
//                    @Override
//                    protected void setResource(Bitmap resource) {
//                        Bitmap blur = ImageBlurUtil.doBlur(getContext(), resource, 10, 1.5f);
//                        iv_cover.setImageBitmap(blur);
//                    }
//
//                    @Override
//                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
//                        super.onLoadFailed(e, errorDrawable);
//                    }
//                });

//        if (!isHttpVideo) {
//            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//            retriever.setDataSource(videoUrl);
//            Bitmap bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
//            if (null != bitmap) {
//                bitmap = ImageBlurUtil.doBlur(getContext(), bitmap, 10, 1.5f);
//                iv_cover.setImageBitmap(bitmap);
//            }
//        } else {
//            Glide.with(getContext()).load(coverUrl).asBitmap().into(
//                    new BitmapImageViewTarget(iv_cover) {
//                        @Override
//                        protected void setResource(Bitmap resource) {
//                            Bitmap blur = ImageBlurUtil.doBlur(getContext(), resource, 10, 1.5f);
//                            iv_cover.setImageBitmap(blur);
//                        }
//
//                        @Override
//                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
//                            super.onLoadFailed(e, errorDrawable);
//                        }
//                    });
//        }

        getView().findViewById(R.id.root).setOnClickListener(this);
        setOnDismissListener(this);

        textureView.setSurfaceTextureListener(this);

    }

    private void initTimer() {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (null != mediaPlayer) {
                    if (mediaPlayer.isPlaying() && sb_bar.isPressed() == false && null != mHandler)
                        mHandler.sendEmptyMessage(0);
                }
            }
        };
        mHandler = new MyHandler(this);
        mTimer.schedule(mTimerTask, 0, 20);
    }

    private void updateSeekBar() {
        if (mediaPlayer != null) {
            try {
                currentProgress = mediaPlayer.getCurrentPosition();
                sb_bar.setProgress(currentProgress);
            } catch (Exception e) {
            }
        }
    }

    private void setListener() {
        sb_bar.setOnSeekBarChangeListener(this);
        textureView.setOnClickListener(this);
        tv_control.setOnClickListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        //页面每次onResume都会进入这里
        if (null == mediaPlayer) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.setOnErrorListener(this);
            try {
                mediaPlayer.setDataSource(getContext(), Uri.parse(videoUrl));
                mediaPlayer.setSurface(new Surface(surface));
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                if (videoUrl.startsWith("http"))
                    mediaPlayer.prepareAsync();
                else {
                    tv_control.setActivated(true);
                    mediaPlayer.prepare();
                }
            } catch (IOException e) {
                Toast.makeText(getContext(), "视频解析出错", Toast.LENGTH_SHORT).show();
                tv_control.setActivated(false);
                e.printStackTrace();
            }
        } else {
            try {
                // 恢复mediaPlayer状态
                mediaPlayer.setSurface(new Surface(surface));
                if (tv_control.isActivated()) {
                    // 播放状态
                    mediaPlayer.start();
                } else {
                    // 如果之前是暂停状态，恢复后会出现黑屏，因此先start(),再pause(),消除黑屏
                    mediaPlayer.start();
                    rl_video.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mediaPlayer.pause();
                            tv_control.setActivated(false);
                        }
                    }, 50);
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "视频解析出错", Toast.LENGTH_SHORT).show();
                tv_control.setActivated(false);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // 销毁时，先暂停，然后页面重建时再恢复
        if (null != mediaPlayer) {
            mediaPlayer.pause();
        }
        // 一定要返回false
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (width == 0 || height == 0) return;
        if (width > height) {
            float ratio = height * 1.0f / width;
            videoWidth = mScreenWidth;
            videoHeight = (int) (mScreenWidth * ratio);
            rl_video.getLayoutParams().width = videoWidth;
            rl_video.getLayoutParams().height = videoHeight;
            rl_video.requestLayout();
        } else {
            float video_ratio = width * 1.0f / height;
            float screen_ratio = mScreenWidth * 1.0f / mScreenHeight;
            if (video_ratio < screen_ratio) {
                videoHeight = mScreenHeight;
                videoWidth = (int) (videoHeight * video_ratio);
                rl_video.getLayoutParams().height = videoHeight;
                textureView.getLayoutParams().height = videoHeight;
                textureView.getLayoutParams().width = videoWidth;
                rl_video.requestLayout();
                textureView.requestLayout();
            } else {
                videoWidth = mScreenWidth;
                videoHeight = (int) (videoWidth / video_ratio);
                rl_video.getLayoutParams().width = videoWidth;
                rl_video.getLayoutParams().height = videoHeight;
                rl_video.requestLayout();
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // MediaPlayer ready
        setListener();
        initTimer();
        sb_bar.setMax(mediaPlayer.getDuration());
        sb_bar.setProgress(currentProgress);
        sb_bar.setSecondaryProgress(mediaPlayer.getDuration());
        mediaPlayer.seekTo(currentProgress);
        // 第一次初始化 或 处于播放状态时，播放视频
        if (!isPrepared || tv_control.isActivated())
            mediaPlayer.start();
        sb_bar.setEnabled(true);
        isPrepared = true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // MediaPlayer  finish
        tv_control.setActivated(false);
        currentProgress = 0;
        sb_bar.setProgress(currentProgress);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // 网络视频的缓冲。每次调用mediaPlayer.start()都会进到这里
        if (isPrepared) {
            pv_progress.setCurrentProgress(percent);
            if (percent >= 100) {
                pv_progress.setVisibility(View.GONE);
                tv_control.setActivated(true);
            } else {
                pv_progress.setVisibility(View.VISIBLE);
                tv_control.setActivated(false);
            }
        }


    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getContext(), "视频播放出错", Toast.LENGTH_SHORT).show();
        release();
        sb_bar.setEnabled(false);
        tv_control.setEnabled(false);
        textureView.setEnabled(false);
        return false;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mediaPlayer.pause();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        currentProgress = seekBar.getProgress();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaPlayer.seekTo(currentProgress, MediaPlayer.SEEK_CLOSEST);
        } else {
            mediaPlayer.seekTo(currentProgress);
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (tv_control.isActivated())
            mediaPlayer.start();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.root) {
            boolean isControllerVisible = controller.getVisibility() == View.VISIBLE;
            controller.setVisibility(isControllerVisible ? View.GONE : View.VISIBLE);

        } else if (i == R.id.tv_control) {
            if (null != mediaPlayer) {
                if (mediaPlayer.isPlaying()) {
                    tv_control.setActivated(false);
                    mediaPlayer.pause();
                } else {
                    tv_control.setActivated(true);
                    mediaPlayer.start();
                }
            }

        } else if (i == R.id.textureview) {
            if (textureView.getHeight() == mScreenHeight) {
                boolean isVisible = controller.getVisibility() == View.VISIBLE;
                controller.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            } else if (null != mediaPlayer) {
                if (mediaPlayer.isPlaying()) {
                    tv_control.setActivated(false);
                    mediaPlayer.pause();
                } else {
                    tv_control.setActivated(true);
                    mediaPlayer.start();
                }
            }

        }
    }

    private void release() {
        if (mHandler != null) {
            mHandler = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDismiss() {
        release();
    }

    @Override
    public void doEnterAnim(final View contentView, long animDuration) {
        controller.setVisibility(View.INVISIBLE);
        final View target = contentView.findViewById(R.id.root);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
        valueAnimator.setDuration(animDuration);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float current = (float) animation.getAnimatedValue();
                int currentH = (int) (mScreenHeight * current);
                target.getLayoutParams().height = currentH;
                target.requestLayout();
                target.setTranslationY(mScreenHeight / 2 - currentH / 2);
                if (currentH < videoHeight) transformFrame(currentH);
                if (current >= 1) controller.setVisibility(View.VISIBLE);
            }
        });
        valueAnimator.start();
    }

    @Override
    public void doExitAnim(View contentView, long animDuration) {
        controller.setVisibility(View.INVISIBLE);
        final View target = contentView.findViewById(R.id.root);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1f, 0);
        valueAnimator.setDuration(animDuration);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float current = (float) animation.getAnimatedValue();
                int currentH = (int) (mScreenHeight * current);
                target.getLayoutParams().height = currentH;
                target.requestLayout();
                target.setTranslationY(mScreenHeight / 2 - currentH / 2);
                if (currentH < videoHeight) transformFrame(currentH);
            }
        });
        valueAnimator.start();
    }

    private void transformFrame(int currentH) {
        Matrix matrix = new Matrix();
        float scale = videoHeight * 1f / currentH;
        matrix.setScale(1, scale, videoWidth / 2, currentH / 2);
        textureView.setTransform(matrix);
        textureView.postInvalidate();
    }
}