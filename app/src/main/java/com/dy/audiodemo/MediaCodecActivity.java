package com.dy.audiodemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;

import com.dy.audiodemo.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class MediaCodecActivity extends AppCompatActivity implements View.OnClickListener{
    private int recBufSize, playBufSize;
    private boolean isRecording;
    private AudioRecord mAudioRecord;
    private static final int sampleRateInHz = 44100;//采样率
    private static final int channelInConfig = AudioFormat.CHANNEL_IN_STEREO;//通道数
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;//位数

    private Chronometer mChronometer;
    private SeekBar mSeekBarSave;
    private AudioEncoder audioEncoder;
    private MediaPlayer mediaPlayer;
    private ArrayList<byte[]> chunkPCMDataContainer;//PCM数据块容器
    private TextView tvPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);
        initView();
    }

    private void initView() {

        chunkPCMDataContainer = new ArrayList<>();
        mChronometer = (Chronometer) findViewById(R.id.chronometer);

        mSeekBarSave = (SeekBar) findViewById(R.id.seekBarSave);

        tvPercent = (TextView) findViewById(R.id.tvPercent);
        Button btnRecord = (Button) findViewById(R.id.btnRecord);
        Button btnStop = (Button) findViewById(R.id.btnStop);
        Button btnPlay = (Button) findViewById(R.id.btnPlay);
        Button btnStopAAC = (Button) findViewById(R.id.btnStopAAC);
        btnRecord.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnStopAAC.setOnClickListener(this);

        recBufSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelInConfig, audioFormat);


        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz, channelInConfig, audioFormat, recBufSize);

        audioEncoder = new AudioEncoder();

        mSeekBarSave.setMax(100);

        mSeekBarSave.setProgress(0);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRecord:
                isRecording = true;

                //计时器清零
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
                new RecordThread().start();
                break;
            case R.id.btnStop:
                isRecording = false;

                mChronometer.stop();
                audioEncoder.setEncodeType(MediaFormat.MIMETYPE_AUDIO_AAC);
                audioEncoder.setPCMData(chunkPCMDataContainer, Environment.getExternalStorageDirectory().getPath() + "/test.aac");

                audioEncoder.prepare();
                audioEncoder.startAsync();
                audioEncoder.setOnCompleteListener(new AudioEncoder.OnCompleteListener() {
                    @Override
                    public void completed() {
                        audioEncoder.release();
                    }
                });
                audioEncoder.setOnProgressListener(new AudioEncoder.OnProgressListener() {
                    @Override
                    public void progress(final int progress) {

                        mSeekBarSave.setProgress(progress);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvPercent.setText(progress + "%");
                            }
                        });

                    }
                });

                break;
            case R.id.btnPlay:
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(new File(Environment.getExternalStorageDirectory().getPath(), "test.aac").getPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    try {
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mediaPlayer.start();


                }
                isRecording = false;

                mediaPlayer.start();
                break;
            case R.id.btnStopAAC:
                mediaPlayer.stop();

                isRecording = false;
                break;
        }
    }


    private class RecordThread extends Thread {

        @Override
        public void run() {

            byte[] buffer = new byte[recBufSize];
            mAudioRecord.startRecording();
            while (isRecording) {
                int bufferReadResult = mAudioRecord.read(buffer, 0, recBufSize);
                byte[] tempBuffer = new byte[bufferReadResult];
                System.arraycopy(buffer, 0, tempBuffer, 0, bufferReadResult);
                chunkPCMDataContainer.add(tempBuffer);

            }
            mAudioRecord.stop();
        }
    }


}
