package ru.nxdomain.camera.codec.audio;

import androidx.annotation.NonNull;

import ru.nxdomain.camera.codec.Codec;
import ru.nxdomain.camera.codec.Frame;

public class AudioDecoder extends Codec<byte[]> implements AudioFrame.AudioRecycler {
    private Callback mCallback;
    private long mHandler;

    public interface Callback {
        void onDecodedSample(short[] bytes, AudioDecoder decoder);
    }

    public AudioDecoder(int sampleRate, int channels, Callback callback) {
        super();
        mCallback = callback;
        setup(sampleRate, channels);
    }

    protected native void setup(int sampleRate, int channels)
            throws RuntimeException;

    protected native void decode(byte[] bytes)
            throws RuntimeException;

    protected native void cleanup();

    protected void onFrame(short[] bytes) {
        mCallback.onDecodedSample(bytes, this);
    }

    @Override
    protected void codeFrame(@NonNull Frame<byte[]> frame) {
        decode(frame.bytes());
    }

    @Override
    public void addBuffer(short[] bytes) {
        // TODO
    }

    @Override
    public void release() {
        super.release();
        cleanup();
    }
}
