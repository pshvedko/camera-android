package ru.nxdomain.camera.codec.audio;

import androidx.annotation.NonNull;

import ru.nxdomain.camera.codec.Frame;

public class AudioFrame extends Frame<short[]> {
    private final AudioRecycler mRecycler;

    public interface AudioRecycler {
        void addBuffer(short[] bytes);
    }

    public AudioFrame(short[] sample, @NonNull AudioRecycler recycler) {
        super(sample);
        mRecycler = recycler;
    }

    @Override
    public void recycle() {
        mRecycler.addBuffer(bytes());
    }
}
