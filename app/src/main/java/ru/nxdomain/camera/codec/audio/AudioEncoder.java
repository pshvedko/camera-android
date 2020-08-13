package ru.nxdomain.camera.codec.audio;

import androidx.annotation.NonNull;

import ru.nxdomain.camera.codec.Codec;
import ru.nxdomain.camera.codec.Frame;

public final class AudioEncoder extends Codec<short[]> {
    private Callback mCallback;
    private long mHandler;

    public interface Callback {
        void onEncodedSample(byte[] bytes, long id);
    }

    public AudioEncoder(int sampleRate, int channels, Callback callback) {
        super();
        mCallback = callback;
        setup(sampleRate, channels);
    }

    protected native void setup(int sampleRate, int channels)
            throws RuntimeException;

    protected native void encode(short[] bytes)
            throws RuntimeException;

    protected native void cleanup();

    protected void onFrame(byte[] bytes, long id) {
        mCallback.onEncodedSample(bytes, id);
    }

    @Override
    protected void codeFrame(@NonNull Frame<short[]> frame) {
        encode(frame.bytes());
    }

    @Override
    public void release() {
        super.release();
        cleanup();
    }
}
