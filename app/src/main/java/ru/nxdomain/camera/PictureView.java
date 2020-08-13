package ru.nxdomain.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import ru.nxdomain.camera.codec.Ratio;

public class PictureView extends SurfaceView {
    private int aw = 4;
    private int ah = 3;

    public PictureView(Context context) {
        super(context);
    }

    public PictureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PictureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int width, int height) {
        int w = MeasureSpec.getSize(width);
        int h = MeasureSpec.getSize(height);
        if (w > h)
            w = h * aw / ah;
        else
            h = w * aw / ah;
        w = MeasureSpec.makeMeasureSpec(w, MeasureSpec.getMode(width));
        h = MeasureSpec.makeMeasureSpec(h, MeasureSpec.getMode(height));
        super.onMeasure(w, h);
    }

    public void setRatio(@NonNull Ratio a) {
        aw = a.width;
        ah = a.height;
        setLayoutParams(getLayoutParams());
    }

    public boolean isSquare() {
        return aw == ah;
    }
}
