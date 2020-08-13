package ru.nxdomain.camera;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.nxdomain.camera.codec.Flow;
import ru.nxdomain.camera.codec.Ratio;
import ru.nxdomain.camera.codec.Size;


public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener,
        AdapterView.OnItemClickListener, Flow.Signal {
    static {
        System.loadLibrary("camera");
    }

    private static final String TAG = "VirtualCamera";
    private PictureView mSurfaceView;
    private RelativeLayout mControlView;
    private Camera mCamera;
    private int mCameraId;
    private SquareView mSquareView;
    private ListView mListView;
    private static final String[] PERMISSION = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
    };
    private Size[] mCameraSize = new Size[Camera.getNumberOfCameras()];
    private Flow mStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: savedInstanceState=" + savedInstanceState);
        super.onCreate(savedInstanceState);
        Hello hello = new Hello("World!");
        Log.i(TAG, hello.greetings());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        mSurfaceView = findViewById(R.id.surface);
        mControlView = findViewById(R.id.control);
        mSquareView = findViewById(R.id.square);
        mListView = findViewById(R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }


    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArraySet<String> permission = new ArraySet<>();
            for (String p : PERMISSION) {
                if (ActivityCompat.checkSelfPermission(this, p)
                        != PackageManager.PERMISSION_GRANTED)
                    permission.add(p);
            }
            if (permission.size() > 0) {
                ActivityCompat.requestPermissions(this, permission.toArray(new String[0]), 1);
                return;
            }
        }
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.setKeepScreenOn(true);
        holder.addCallback(this);
        mStream = new Flow(this);
        mStream.start();
    }


    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        if (mStream != null)
            mStream.release();
        super.onStop();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grants) {
        Log.i(TAG, "onRequestPermissionsResult: requestCode=" + requestCode +
                " permissions=" + Arrays.toString(permissions) +
                " grants=" + Arrays.toString(grants));
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode != 1)
            return;
        if (permissions.length == 0)
            return;
        if (grants.length == 0)
            return;
        for (int g : grants) {
            if (g != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
        }
        recreate();
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration conf) {
        Log.i(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(conf);
        if (!mSurfaceView.isSquare())
            return;
        SurfaceHolder holder = mSurfaceView.getHolder();
        surfaceChanged(holder, PixelFormat.RGB_888, 0, 0);
    }


    @Override
    public void onClick(@NonNull final View view) {
        Log.i(TAG, "onClick: view=" + view);
        switch (view.getId()) {
            case R.id.camera:
                mCameraId++;
                mCameraId %= Camera.getNumberOfCameras();
                SurfaceHolder holder = mSurfaceView.getHolder();
                surfaceDestroyed(holder);
                surfaceCreated(holder);
                surfaceChanged(holder, PixelFormat.RGB_888, 0, 0);
                mListView.setVisibility(View.GONE);
                break;
            case R.id.setting:
                if (mListView.getVisibility() == View.VISIBLE) {
                    mListView.setVisibility(View.GONE);
                    break;
                }
                List<Size> sizes = new ArrayList<>();
                for (Camera.Size s : mCamera.getParameters().getSupportedPreviewSizes()) {
                    sizes.add(new Size(s));
                }
                Collections.sort(sizes);
                ArrayAdapter<Size> arrayAdapter = new ArrayAdapter<Size>(this,
                        android.R.layout.simple_list_item_single_choice,
                        sizes) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        view.setTypeface(view.getTypeface(), Typeface.BOLD);
                        view.setTextColor(Color.WHITE);
                        view.setAlpha(.5f);
                        return view;
                    }
                };
                int checked = sizes.indexOf(new Size(mCamera.getParameters().getPreviewSize()));
                mListView.setAdapter(arrayAdapter);
                mListView.setItemChecked(checked, true);
                mListView.smoothScrollToPosition(checked);
                mListView.setVisibility(View.VISIBLE);
                mListView.setOnItemClickListener(this);
                break;
        }
        if (!mStream.isConnected())
            return;
        mControlView.setVisibility(View.VISIBLE);
        mControlView.animate().alpha(0).setDuration(1000).setStartDelay(9000).
                setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (animator.isRunning()) {
                            mControlView.setVisibility(View.GONE);
                            mListView.setVisibility(View.GONE);
                        } else
                            mControlView.setVisibility(View.VISIBLE);
                        mControlView.setAlpha(1);
                    }
                }).start();
    }


    @Override
    public void onItemClick(@NonNull AdapterView<?> parent, View view, int position, long id) {
        Object size = parent.getItemAtPosition(position);
        if (size instanceof Size) {
            Log.i(TAG, "onItemClick: position=" + position + " id=" + id + " size=" + size);
            mCameraSize[mCameraId] = (Size) size;
            SurfaceHolder holder = mSurfaceView.getHolder();
            surfaceDestroyed(holder);
            surfaceCreated(holder);
            surfaceChanged(holder, PixelFormat.RGB_888, 0, 0);
        }
        onClick(view);
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated: holder=" + holder);
        if (!mStream.isConnected())
            mCameraId = 0;
        try {
            mCamera = Camera.open(mCameraId);
        } catch (Exception e) {
            return;
        }
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            return;
        }
        Camera.Parameters cameraParameters = mCamera.getParameters();
        int focus = 0;
        for (String f : cameraParameters.getSupportedFocusModes()) {
            Log.i(TAG, "mode=" + f);
            int mode = 1;
            switch (f) {
                case Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE:
                    mode++;
                case Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO:
                    mode++;
                case Camera.Parameters.FOCUS_MODE_MACRO:
                    mode++;
                case Camera.Parameters.FOCUS_MODE_FIXED:
                    mode++;
                case Camera.Parameters.FOCUS_MODE_INFINITY:
                    mode++;
                case Camera.Parameters.FOCUS_MODE_AUTO:
                    mode++;
            }
            if (focus < mode) {
                focus = mode;
                cameraParameters.setFocusMode(f);
                Log.i(TAG, "^^^^");
            }
        }
        if (mStream.isConnected() && mCameraSize[mCameraId] != null) {
            cameraParameters.setPreviewSize(mCameraSize[mCameraId].width, mCameraSize[mCameraId].height);
        } else {
            List<Size> sizes = new ArrayList<>();
            for (Camera.Size s : cameraParameters.getSupportedPreviewSizes())
                sizes.add(new Size(s));
            Collections.sort(sizes);
            Ratio a = new Ratio(getWindow().getDecorView().getWidth(), getWindow().getDecorView().getHeight());
            for (Size s : sizes) {
                Ratio b = s.ratio();
                Log.i(TAG, "size=" + s.width + "x" + s.height + " ratio=" + b);
                if (mStream.isConnected()) {
                    if (a.equals(b) && s.width <= 1280) {
                        Log.i(TAG, "^^^^");
                        cameraParameters.setPreviewSize(s.width, s.height);
                        mCameraSize[mCameraId] = s;
                    }
                } else if (s.width <= 320) {
                    Log.i(TAG, "^^^^");
                    cameraParameters.setPreviewSize(s.width, s.height);
                }
            }
        }
        int format = cameraParameters.getPreviewFormat();
        for (int p : cameraParameters.getSupportedPreviewFormats()) {
            Log.i(TAG, "format=" + p);
            if (p == format)
                Log.i(TAG, "^^^^");
        }
        final Size size = new Size(cameraParameters.getPreviewSize());
        mCamera.setParameters(cameraParameters);
        int length = ImageFormat.getBitsPerPixel(format);
        length *= size.height;
        length *= size.width;
        length /= 8;
        mStream.setSize(size);
        mCamera.addCallbackBuffer(new byte[length]);
        mCamera.addCallbackBuffer(new byte[length]);
        mCamera.setPreviewCallbackWithBuffer(mStream);
        mCamera.startPreview();
        mControlView.setVisibility(View.GONE);
        if (mStream.isConnected())
            mSquareView.setVisibility(View.GONE);
        else
            mSquareView.setVisibility(View.VISIBLE);
        mSurfaceView.setRatio(size.ratio());
    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged: holder=" + holder + " format=" + format + " width=" + width + " height=" + height);
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int orientation = 90 * getWindowManager().getDefaultDisplay().getRotation();
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            orientation += info.orientation;
            orientation %= 360;
            orientation *= -1;
        } else {  // back-facing
            orientation *= -1;
            orientation += info.orientation;
        }
        orientation += 360;
        orientation %= 360;
        Log.i(TAG, "orientation=" + info.orientation + ":" + orientation);
        mCamera.setDisplayOrientation(orientation);
    }


    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: holder=" + holder);
        mCamera.stopPreview();
        mCamera.cancelAutoFocus();
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.release();
    }


    @Override
    public void onFlow() {
        Log.i(TAG, "onFlow");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int cameraId = mCameraId;
                SurfaceHolder holder = mSurfaceView.getHolder();
                surfaceDestroyed(holder);
                surfaceCreated(holder);
                surfaceChanged(holder, PixelFormat.RGB_888, 0, 0);
                mCameraId = cameraId;
            }
        });
    }


    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        super.onBackPressed();
        finish();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i(TAG, "onWindowFocusChanged: hasFocus=" + hasFocus);
        super.onWindowFocusChanged(hasFocus);
    }


    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }


    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        Toast toast = Toast.makeText(this, "It means destruction worked!", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 100);
        toast.show();
    }
}