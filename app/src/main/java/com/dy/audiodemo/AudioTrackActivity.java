package com.dy.audiodemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class AudioTrackActivity extends AppCompatActivity {
    private static final int PERMISSION_CODE = 12;
    private Button btnStart;
    private Button btnStop;

    private int recBufSize, playBufSize;
    private static final int sampleRateInHz = 44100;//采样率
    private static final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;//位数
    private PCMAudioTrackThread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_track);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mThread = new PCMAudioTrackThread();
                mThread.start();
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mThread != null) {
                    mThread.free();
                }
            }
        });
    }


    class PCMAudioTrackThread extends Thread {
        private AudioTrack mAudioTrack;
        private int outBufferSize;
        private byte[] outBytes;
        private boolean isRunning;
        private InputStream mInputStream;

        public PCMAudioTrackThread() {
            try {
                mInputStream = getResources().getAssets().open("yilian.wav");
            } catch (IOException e) {
                e.printStackTrace();
            }
            isRunning = true;
            outBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                    channelConfig, audioFormat, outBufferSize, AudioTrack.MODE_STREAM);

            outBytes = new byte[outBufferSize];
        }

        public void free() {
            isRunning = false;
            interrupt();
            try {
                join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] temp = null;
            mAudioTrack.play();
            while (isRunning) {
                try {
                    mInputStream.read(outBytes);
                    temp = outBytes.clone();
                    mAudioTrack.write(temp, 0, temp.length);
                    System.out.println("正在播放");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mAudioTrack.stop();
            mAudioTrack.release();
            System.out.println("停止播放");
            temp = null;
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
