package com.dy.audiodemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_CODE = 12;
    private Button btnRecord;
    private Button btnStop;
    private Button btnExit;
    private TextView textView;
    private SeekBar skbVolume;
    private int recBufSize, playBufSize;
    private static final int sampleRateInHz = 44100;//采样率
    private static final int channelInConfig = AudioFormat.CHANNEL_IN_MONO;//通道数
    private static final int channelOutConfig = AudioFormat.CHANNEL_OUT_MONO;
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;//位数

    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;

    private boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                //上次用户已经拒绝过申请，这次给用户提示是否给予权限
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "申请成功", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initView() {

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnExit = (Button) findViewById(R.id.btnExit);
        textView = (TextView) findViewById(R.id.textView);
        skbVolume = (SeekBar) findViewById(R.id.skbVolume);

        btnRecord.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnExit.setOnClickListener(this);

        recBufSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelInConfig, audioFormat);
        playBufSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelOutConfig, audioFormat);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz, channelInConfig, audioFormat, recBufSize);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelOutConfig, audioFormat, playBufSize, AudioTrack.MODE_STREAM);

        skbVolume.setMax(100);
        skbVolume.setProgress(70);

        setVolume(0.7f);

        skbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float ovl = (float) (seekBar.getProgress()) / (float) (seekBar.getMax());
                setVolume(ovl);
            }
        });


    }

    private void setVolume(float gain) {
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioTrack.setVolume(gain);
        } else {
            mAudioTrack.setStereoVolume(gain, gain);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRecord:
                isRecording = true;
                new RecordPlayThread().start();
                break;
            case R.id.btnStop:
                isRecording = false;
                break;
            case R.id.btnExit:
                isRecording = false;
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    class RecordPlayThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[recBufSize];

            mAudioRecord.startRecording();
            mAudioTrack.play();

            while (isRecording) {
                int bufferReadResult = mAudioRecord.read(buffer, 0, recBufSize);

                byte[] tempBuffer = new byte[bufferReadResult];
                System.arraycopy(buffer, 0, tempBuffer, 0, bufferReadResult);

                mAudioTrack.write(tempBuffer, 0, tempBuffer.length);

            }

            mAudioTrack.stop();
            mAudioRecord.stop();
        }
    }
}
