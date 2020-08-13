package ru.nxdomain.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SquareView extends View {

    public SquareView(Context context) {
        super(context);
    }

    public SquareView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onMeasure(int width, int height) {
        int size = Math.min(width, height);
        super.onMeasure(size, size);
    }
}
