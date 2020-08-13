package ru.nxdomain.camera.codec.video;

import androidx.annotation.NonNull;

import ru.nxdomain.camera.codec.Codec;
import ru.nxdomain.camera.codec.Frame;
import ru.nxdomain.camera.codec.Size;

public final class VideoEncoder extends Codec<byte[]> {
    private Callback mCallback;
    private long mHandler;

    public interface Callback {
        void onEncodedFrame(byte[] bytes, int flag, long id);

        void onBegin(Size size);
    }

    public VideoEncoder(Size size, int frameRate, int bitRate, Callback callback) {
        super();
        mCallback = callback;
        mCallback.onBegin(size);
        setup(size.width, size.height, frameRate, bitRate);
    }

    protected native void setup(int width, int height, int frameRate, int bitRate)
            throws RuntimeException;

    protected native void encode(byte[] bytes, boolean key)
            throws RuntimeException;

    protected native void cleanup();

    protected void onFrame(byte[] bytes, int flag, long id) {
        mCallback.onEncodedFrame(bytes, flag, id);
    }

    @Override
    protected void codeFrame(@NonNull Frame<byte[]> frame) {
        encode(frame.bytes(), false);
    }

    @Override
    public void release() {
        super.release();
        cleanup();
    }
}
