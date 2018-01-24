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

package com.errang.rximagepicker.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;

import com.errang.rximagepicker.R;


/**
 * a circle progress view with progress percent
 * Created by zengp on 2017/6/27.
 */

public class ProgressView extends AppCompatImageView {

    private Context mContext;

    private int mProgressRadius, mProgressWidth; // 半径,宽度
    private int mProgressLoadedColor, mProgressUnLoadColor, mTextColor; // 颜色
    private float mTextSize; // 字体大小
    private boolean isShowProgressText; // 是否显示进度

    private float currentProgress = 0;

    public ProgressView(Context context) {
        this(context, null);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressView, defStyleAttr, 0);
        mProgressLoadedColor = a.getColor(R.styleable.ProgressView_progress_loaded_color, 0xff999999);
        mProgressUnLoadColor = a.getColor(R.styleable.ProgressView_progress_unload_color, 0xffdddddd);
        mTextColor = a.getColor(R.styleable.ProgressView_progress_text_color, 0xff999999);
        mProgressRadius = a.getDimensionPixelOffset(R.styleable.ProgressView_progress_radius, 40);
        mProgressWidth = a.getDimensionPixelOffset(R.styleable.ProgressView_progress_width, 6);
        mTextSize = a.getDimensionPixelOffset(R.styleable.ProgressView_progress_text_size, 16);
        isShowProgressText = a.getBoolean(R.styleable.ProgressView_progress_show_text, true);
        if (mProgressRadius <= 0)
            mProgressRadius = 40;
        if (mProgressWidth <= 0)
            mProgressWidth = 6;
        if (mTextSize <= 0)
            mTextSize = 16;
        a.recycle();

    }

    public void setCurrentProgress(float currentProgress) {
        this.currentProgress = currentProgress;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED)
            measuredWidth = getPaddingLeft() + getPaddingRight() + 2 * mProgressRadius;
        if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED)
            measureHeight = getPaddingTop() + getPaddingBottom() + 2 * mProgressRadius;

        setMeasuredDimension(measuredWidth, measureHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // draw view bg
        Drawable bgDrawable = getBackground();
        if (null != bgDrawable) {
            bgDrawable.draw(canvas);
        }

        int viewWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        if (viewWidth <= 0 || viewHeight <= 0) {
            Log.w("ProgressView", "no validate area !");
        } else {
            if (mProgressRadius > viewWidth || mProgressRadius > viewHeight) {
                // radius out of bound check
                mProgressRadius = Math.min(viewWidth, viewHeight);
            }
            // width out of bound check
            if (mProgressWidth > mProgressRadius)
                mProgressWidth = mProgressRadius;
            // update current deg
            double currentRadian = 2 * Math.PI * currentProgress / 100;
            int currentDeg = (int) (360 * currentProgress / 100);
            // update four points x y
            int start_1_x = getPaddingLeft() + viewWidth / 2 + mProgressRadius;
            int start_1_y = getPaddingTop() + viewHeight / 2;
            int start_2_x = start_1_x - mProgressWidth;
            int start_2_y = start_1_y;
            int end_1_x = (int) (getPaddingLeft() + viewWidth / 2 + mProgressRadius * Math.cos(currentRadian));
            int end_1_y = (int) (getPaddingTop() + viewHeight / 2 + mProgressRadius * Math.sin(currentRadian));
            int end_2_x = (int) (getPaddingLeft() + viewWidth / 2 + (mProgressRadius - mProgressWidth) * Math.cos(currentRadian));
            int end_2_y = (int) (getPaddingTop() + viewHeight / 2 + (mProgressRadius - mProgressWidth) * Math.sin(currentRadian));
            // calculate square area for circle
            float outLeft = getPaddingLeft() + (viewWidth - 2 * mProgressRadius) / 2;
            float outTop = getPaddingTop() + (viewHeight - 2 * mProgressRadius) / 2;
            float outRight = getPaddingLeft() + viewWidth / 2 + mProgressRadius;
            float outBottom = getPaddingTop() + viewHeight / 2 + mProgressRadius;
            float innerLeft = getPaddingLeft() + (viewWidth - 2 * (mProgressRadius - mProgressWidth)) / 2;
            float innerTop = getPaddingTop() + (viewHeight - 2 * (mProgressRadius - mProgressWidth)) / 2;
            float innerRight = getPaddingLeft() + viewWidth / 2 + (mProgressRadius - mProgressWidth);
            float innerBottom = getPaddingTop() + viewHeight / 2 + (mProgressRadius - mProgressWidth);
            float averageLeft = getPaddingLeft() + (viewWidth - 2 * (mProgressRadius - mProgressWidth / 2)) / 2;
            float averageTop = getPaddingTop() + (viewHeight - 2 * (mProgressRadius - mProgressWidth / 2)) / 2;
            float averageRight = getPaddingLeft() + viewWidth / 2 + (mProgressRadius - mProgressWidth / 2);
            float averageBottom = getPaddingTop() + viewHeight / 2 + (mProgressRadius - mProgressWidth / 2);

            RectF outRectF = new RectF(outLeft, outTop, outRight, outBottom);
            RectF innerRectF = new RectF(innerLeft, innerTop, innerRight, innerBottom);
            RectF averageRectF = new RectF(averageLeft, averageTop, averageRight, averageBottom);

            // draw unload circle progress background
            Paint defaultPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            defaultPaint.setColor(mProgressUnLoadColor);
            defaultPaint.setStyle(Paint.Style.STROKE);
            defaultPaint.setStrokeWidth(mProgressWidth);
            canvas.drawArc(averageRectF, 0, 360, false, defaultPaint);

            // draw loaded circle progress background
            Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            progressPaint.setColor(mProgressLoadedColor);
            if (currentProgress > 0 && currentProgress < 100) {
                // create a path
                Path path_all = new Path();
                path_all.moveTo(start_2_x, start_2_y);
                path_all.lineTo(start_1_x, start_1_y);
                path_all.arcTo(outRectF, 0, currentDeg, false);
                path_all.lineTo(end_2_x, end_2_y);
                if (mProgressRadius > mProgressWidth) {
                    path_all.arcTo(innerRectF, currentDeg, -currentDeg, false);
                }
                path_all.close();
                progressPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(path_all, progressPaint);
            } else if (currentProgress >= 100) {
                // when the currentProgress arrives at or over 100%, a whole circle only be drawn
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setStrokeWidth(mProgressWidth);
                // square RectF must be correctly calculated！
                canvas.drawArc(averageRectF, 0, 360, false, progressPaint);
            }

            // draw text
            if (isShowProgressText) {
                // text out of bound check
                int textSize = (int) mTextSize;
                TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                textPaint.setTextSize(textSize);
                textPaint.setColor(mTextColor);
                int totalTextW = (int) StaticLayout.getDesiredWidth("100%", 0, 4, textPaint);
                if (mProgressRadius == mProgressWidth) {
                    if (totalTextW > mProgressRadius) {
                        textSize = mProgressRadius / 2;
                    }
                } else if (totalTextW > mProgressRadius - mProgressWidth) {
                    textSize = (mProgressRadius - mProgressWidth) / 2;
                }
                textPaint.setTextSize(textSize);
                String text = (int) currentProgress + "%";
                int textW = (int) StaticLayout.getDesiredWidth(text, 0, text.length(), textPaint);
                float text_start_x = getPaddingLeft() + 1.01f * (viewWidth - textW) / 2;
                float text_start_y = getPaddingTop() + 0.95f * (viewHeight + textSize) / 2;
                canvas.drawText(text, text_start_x, text_start_y, textPaint);
            }
            canvas.save();
            canvas.restore();
        }
    }

    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
