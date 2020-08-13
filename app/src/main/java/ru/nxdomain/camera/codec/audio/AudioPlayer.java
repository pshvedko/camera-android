package ru.nxdomain.camera.codec.audio;

import android.media.AudioTrack;

import ru.nxdomain.camera.codec.Codec;
import ru.nxdomain.camera.codec.Frame;

public class AudioPlayer extends Codec<short[]> {
    private final AudioTrack mTrack;

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
        play(frame.bytes());
    }

    @Override
    public void addFrame(Frame<short[]> frame) {
        if (super.size() > 1)
            frame.recycle();
        else
            super.addFrame(frame);
    }

    private void play(short[] bytes) {
        mTrack.write(bytes, 0, bytes.length);
    }
}
