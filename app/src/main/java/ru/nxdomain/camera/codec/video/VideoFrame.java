package ru.nxdomain.camera.codec.video;

import android.hardware.Camera;

import androidx.annotation.NonNull;

import ru.nxdomain.camera.codec.Frame;

public class VideoFrame extends Frame<byte[]> {
    private final Camera mCamera;

    public VideoFrame(byte[] bytes, @NonNull Camera camera) {
        super(bytes);
        mCamera = camera;
    }

    @Override
    public void recycle() {
        mCamera.addCallbackBuffer(bytes());
    }
}
