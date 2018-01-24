package com.errang.rximagepicker.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by zengp on 2017/7/8.
 */

public class ResizeImageView extends AppCompatImageView {

    public ResizeImageView(Context context) {
        this(context, null);
    }

    public ResizeImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResizeImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        Log.d("ResizeImageView", "drawable null " + (getDrawable() == null));
        if (drawable == null) {
            return; // couldn't resolve the URI
        }

        if (drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            return;     // nothing to draw (empty bounds)
        }

        if (getImageMatrix() == null && getPaddingTop() == 0 && getPaddingLeft() == 0) {
            drawResizeDrawable(drawable, canvas);
        } else {
            final int saveCount = canvas.getSaveCount();
            canvas.save();

            if (getCropToPadding()) {
                final int scrollX = getScrollX();
                final int scrollY = getScrollY();
                canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                        scrollX + getRight() - getLeft() - getPaddingRight(),
                        scrollY + getBottom() - getTop() - getPaddingBottom());
            }

            canvas.translate(getPaddingLeft(), getPaddingTop());

            if (getImageMatrix() != null) {
                canvas.concat(getImageMatrix());
            }
            drawResizeDrawable(drawable, canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * when the image (from local or net) size is too large,ImageView may show the image failure
     * with the warning as follow:
     * <p>
     * OpenGLRenderer: Bitmap too large to be uploaded into a texture (4912x3264, max=4096x4096)
     * <p>
     * this method gives a solution for the problem through resizing the BitmapDrawable if the image
     * size is over the max size.
     *
     * @param drawable
     * @param canvas
     */
    private void drawResizeDrawable(Drawable drawable, Canvas canvas) {

        if (getLayerType() == LAYER_TYPE_SOFTWARE) {
            drawable.draw(canvas);
        } else if (!(drawable instanceof BitmapDrawable)) {
            drawable.draw(canvas);
        } else {
            int maxW = canvas.getMaximumBitmapWidth();
            int maxH = canvas.getMaximumBitmapHeight();
            int bitmapW = drawable.getIntrinsicWidth();
            int bitmapH = drawable.getIntrinsicHeight();
            if (bitmapW > maxW || bitmapH > maxH) {
                // need resize
                float ratio = 0.3f * Math.min(maxW * 1f / bitmapW, maxH * 1f / bitmapH);
                Bitmap resizeBmp = Bitmap.createScaledBitmap(((BitmapDrawable) drawable).getBitmap(),
                        (int) (ratio * bitmapW), (int) (ratio * bitmapH), false);
                drawable = new BitmapDrawable(resizeBmp);
                setImageDrawable(drawable);
            } else {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                drawable.draw(canvas);
            }
        }
    }

    @Override
    public boolean getCropToPadding() {
        return super.getCropToPadding();
    }
}
