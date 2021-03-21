package ru.nxdomain.camera.codec;

import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import ru.nxdomain.camera.codec.audio.AudioDecoder;
import ru.nxdomain.camera.codec.audio.AudioEncoder;
import ru.nxdomain.camera.codec.audio.AudioFrame;
import ru.nxdomain.camera.codec.audio.AudioPlayer;
import ru.nxdomain.camera.codec.audio.AudioRecorder;
import ru.nxdomain.camera.codec.video.CodeReader;
import ru.nxdomain.camera.codec.video.VideoEncoder;
import ru.nxdomain.camera.codec.video.VideoFrame;


public class Flow extends Thread implements Camera.PreviewCallback, VideoEncoder.Callback,
        CodeReader.Callback, AudioRecorder.Callback, AudioEncoder.Callback, AudioDecoder.Callback {

    private String mToken;
    private Socket mSocket;
    private Signal mSignal;
    private Codec<byte[]> mCoder;
    private Codec<byte[]> mSender;
    private AudioPlayer mPlayer;
    private AudioEncoder mEncoder;

    private static final byte VIDEO = 0;
    private static final byte AUDIO = 1;
    private static final byte CONTROL = 2;

    @Override
    public void onRecordSample(short[] sample, AudioRecorder recorder) {
        mEncoder.addFrame(new AudioFrame(sample, recorder));
    }

    @Override
    public void onEncodedSample(byte[] bytes, long id) {
        mSender.addFrame(new AudioSample(bytes, id));
    }

    @Override
    public void onDecodedSample(short[] sample, AudioDecoder decoder) {
        mPlayer.addFrame(new AudioFrame(sample, decoder));
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        mCoder.addFrame(new VideoFrame(bytes, camera));
    }

    @Override
    public void onScannedCode(byte[] bytes) {
        synchronized (this) {
            if (mToken != null)
                return;
            mToken = new String(bytes);
            notify();
        }
    }

    @Override
    public void onEncodedFrame(byte[] bytes, int flag, long id) {
        mSender.addFrame(new VideoSample(bytes, flag, id));
    }

    @Override
    public void onBegin(Size size) {
        mSender.addFrame(new VideoControl(size));
    }


    public interface Signal {
        void onFlow();
    }

    public Flow(Signal signal) {
        mSignal = signal;
    }

    private static final int CODEC_RATE = 48000;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL = 2;
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_STEREO;
    private static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    @Override
    public void run() {
        while (true) {
            if (!open())
                return;
            try {
                DataOutputStream writer = new DataOutputStream(mSocket.getOutputStream());
                writer.writeShort(mToken.length());
                writer.writeBytes(mToken);
                writer.flush();
                mSender = new Sender(writer);
                mSender.start();
            } catch (IOException e) {
                close();
                continue;
            }

            mEncoder = new AudioEncoder(CODEC_RATE, CHANNEL, this);
            mEncoder.start();

            int bufferSizeInBytes = CODEC_RATE * CHANNEL * 2 / 10; // 100ms

            mPlayer = new AudioPlayer(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE, CHANNEL_OUT, FORMAT, bufferSizeInBytes);
            mPlayer.start();

            int frameSizeInSamples = bufferSizeInBytes / 2 / 10; // 10ms

            AudioRecorder recorder = new AudioRecorder(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, CHANNEL_IN, FORMAT, bufferSizeInBytes, this);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.addBuffer(new short[frameSizeInSamples]);
            recorder.start();

            AudioDecoder decoder = new AudioDecoder(CODEC_RATE, CHANNEL, this);
            decoder.start();

            mSignal.onFlow();

            try {
                int i = 0;
                DataInputStream reader = new DataInputStream(mSocket.getInputStream());
                while (true) {
                    switch (reader.readByte()) {
                        case AUDIO:
                            decoder.addFrame(AudioSample.read(reader));
                            break;
                    }
                }
            } catch (IOException ignored) {
            }

            decoder.release();
            recorder.release();
            mPlayer.release();
            mEncoder.release();
            mSender.release();
            if (!close())
                break;
            mSignal.onFlow();
        }
        end();
    }

    synchronized
    private boolean open() {
        while (mSocket == null) {
            mToken = null;
            try {
                wait();
            } catch (InterruptedException e) {
                return false;
            }
            String[] code = mToken.split("\\.");
            if (code.length != 3)
                continue;
            try {
                JSONObject json = new JSONObject(new String(Base64.decode(code[1], Base64.URL_SAFE)));
                JSONArray hosts = json.getJSONArray("host");
                int port = json.getInt("port");
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                final List<InterfaceAddress> networks = new ArrayList<>();
                for (NetworkInterface i : interfaces)
                    if (i.isUp() && !i.isLoopback())
                        for (InterfaceAddress a : i.getInterfaceAddresses())
                            if (a.getAddress() instanceof Inet4Address)
                                networks.add(a);
                Deque<InetAddress> addresses = new ArrayDeque<>();
                for (int i = 0; i < hosts.length(); i++) {
                    InetAddress ip = InetAddress.getByName(hosts.getString(i));
                    if (match(networks, ip))
                        addresses.push(ip);
                    else
                        addresses.add(ip);
                }
                for (InetAddress host : addresses) {
                    try {
                        mSocket = new Socket();
                        mSocket.connect(new InetSocketAddress(host, port), 5000);
                        mSocket.setTcpNoDelay(true);
                        mSocket.setKeepAlive(true);
                        break;
                    } catch (IOException e) {
                        close();
                    }
                }
            } catch (JSONException | SocketException | UnknownHostException ignored) {
            }
        }
        return true;
    }

    private static boolean match(List<InterfaceAddress> networks, InetAddress host) {
        for (InterfaceAddress a : networks)
            if (match(host.getAddress(), a.getAddress().getAddress(), a.getNetworkPrefixLength()))
                return true;
        return false;
    }

    private static boolean match(byte[] ip1, byte[] ip2, int len) {
        return ipv4net(ip1, len) == ipv4net(ip2, len);
    }

    private static int ipv4net(byte[] ip, int len) {
        return ipv4(ip) & (0xFFFFFFFF << len);
    }

    private static int ipv4(byte[] ip) {
        return (ip[0] & 0xFF) << 24 | (ip[1] & 0xFF) << 16 | (ip[2] & 0xFF) << 8 | (ip[3] & 0xFF);
    }

    synchronized
    public void setSize(Size size) {
        end();
        if (mSocket != null)
            mCoder = new VideoEncoder(size, 20, 512 * 8, this);
        else
            mCoder = new CodeReader(size, this);
        mCoder.start();
    }

    synchronized
    public boolean isConnected() {
        return mSocket != null;
    }

    synchronized
    private boolean close() {
        if (mSocket == null)
            return false;
        try {
            mSocket.close();
        } catch (IOException ignored) {
        }
        mSocket = null;
        return true;
    }

    synchronized
    public void end() {
        if (mCoder == null)
            return;
        mCoder.release();
        mCoder = null;
    }

    public void release() {
        close();
        interrupt();
        try {
            join();
        } catch (InterruptedException ignored) {
        }
    }

    private static class AudioSample extends Frame<byte[]> {
        private final long mId;

        public AudioSample(byte[] bytes, long id) {
            super(bytes);
            mId = id;
        }

        public static AudioSample read(@NonNull DataInputStream reader) throws IOException {
            long id = reader.readLong();
            int length = reader.readInt();
            byte[] bytes = new byte[length];
            reader.readFully(bytes);
            return new AudioSample(bytes, id);
        }

        @Override
        public byte[] meta() {
            ByteArrayOutputStream array = new ByteArrayOutputStream(1 + 8 + 4);
            DataOutputStream stream = new DataOutputStream(array);
            try {
                stream.writeByte(AUDIO);
                stream.writeLong(mId);
                stream.writeInt(bytes().length);
                stream.flush();
            } catch (IOException e) {
                return null;
            }
            return array.toByteArray();
        }
    }

    private static class VideoSample extends Frame<byte[]> {
        private final long mId;
        private final int mFlag;

        public VideoSample(byte[] bytes, int flag, long id) {
            super(bytes);
            mId = id;
            mFlag = flag;
        }

        @Override
        public byte[] meta() {
            ByteArrayOutputStream array = new ByteArrayOutputStream(1 + 8 + 4 + 4);
            DataOutputStream stream = new DataOutputStream(array);
            try {
                stream.writeByte(VIDEO);
                stream.writeInt(mFlag);
                stream.writeLong(mId);
                stream.writeInt(bytes().length);
                stream.flush();
            } catch (IOException e) {
                return null;
            }
            return array.toByteArray();
        }
    }

    private static class VideoControl extends Frame<byte[]> {
        private final Size mSize;

        public VideoControl(Size size) {
            super(new byte[0]);
            mSize = size;
        }

        @Override
        public byte[] meta() {
            ByteArrayOutputStream array = new ByteArrayOutputStream(1 + 4 + 4 + 4);
            DataOutputStream stream = new DataOutputStream(array);
            try {
                stream.writeByte(VIDEO | CONTROL);
                stream.writeInt(mSize.width);
                stream.writeInt(mSize.height);
                stream.writeInt(0);
                stream.flush();
            } catch (IOException e) {
                return null;
            }
            return array.toByteArray();
        }
    }

    private static class Sender extends Codec<byte[]> {
        private final DataOutputStream mWriter;

        private Sender(DataOutputStream stream) {
            mWriter = stream;
        }

        @Override
        protected void codeFrame(@NonNull Frame<byte[]> frame) {
            try {
                mWriter.write(frame.meta());
                mWriter.write(frame.bytes());
                mWriter.flush();
            } catch (IOException e) {
                try {
                    mWriter.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
