package ru.nxdomain.camera.codec.audio;

import android.media.AudioTrack;

import ru.nxdomain.camera.codec.Codec;
import ru.nxdomain.camera.codec.Frame;

public class AudioPlayer extends Codec<short[]> {
    public static final int BUFFER_SIZE = 8;
    private final AudioTrack mTrack;
    private boolean mSkip;

    public AudioPlayer(int audioStream, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        super();
        mTrack = new AudioTrack(audioStream, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM);
    }

    @Override
    public void run() {
        mTrack.play();
        super.run();
        mTrack.stop();
        mTrack.release();
    }

    @Override
    protected void codeFrame(Frame<short[]> frame) {
        int n = size();
        if (mSkip) {
            if (n > BUFFER_SIZE)
                return;
            mSkip = false;
        } else if (n > BUFFER_SIZE * 2) {
            mSkip = true;
        } else if (n < BUFFER_SIZE / 2) {
            try {
                stash(BUFFER_SIZE);
            } catch (InterruptedException ignore) {
            }
        }
        play(frame.bytes());
    }

    @Override
    public void addFrame(Frame<short[]> frame) {
        super.addFrame(frame);
    }

    private void play(short[] bytes) {
        mTrack.write(bytes, 0, bytes.length);
    }
}
