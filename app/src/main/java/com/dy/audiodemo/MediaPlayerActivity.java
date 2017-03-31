package com.dy.audiodemo;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class MediaPlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MediaPlayer";
    private TextView text;
    private AppCompatButton btnStart;
    private AppCompatButton btnReset;
    private AppCompatButton btnPause;
    private AppCompatButton btnStop;
    private MediaPlayer mMediaPlayer;
    private boolean isReleased, isPaused;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        initView();

    }

    private void initView() {


        text = (TextView) findViewById(R.id.text);
        btnStart = (AppCompatButton) findViewById(R.id.btnStart);
        btnReset = (AppCompatButton) findViewById(R.id.btnReset);
        btnPause = (AppCompatButton) findViewById(R.id.btnPause);
        btnStop = (AppCompatButton) findViewById(R.id.btnStop);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        btnStart.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        mMediaPlayer = MediaPlayer.create(this, R.raw.robotica_1080);

        final SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mMediaPlayer.setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "错误：what" + what + "extra:" + extra);

                return false;
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                //缓冲区更新
                Log.i(TAG, "更新缓冲区：" + percent + "%");
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(TAG, "播放完毕");
                mMediaPlayer.seekTo(0);
            }
        });
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "准备完毕");
            }
        });
        //如果MediaPlayer实例是由create方法创建的，那么第一次启动播放前不需要再调用prepare（）了，因为create方法里已经调用过了。
//        try {
//            mMediaPlayer.prepare();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                mMediaPlayer.start();
                text.setText("正在播放");
                break;
            case R.id.btnReset:
                if (!isReleased) {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.seekTo(0);
                    }
                }
                break;
            case R.id.btnPause:
                if (mMediaPlayer != null) {
                    if (!isReleased) {
                        if (!isPaused) {
                            mMediaPlayer.pause();
                            isPaused = true;
                            text.setText("暂停中");
                        } else {
                            mMediaPlayer.start();
                            isPaused = false;
                            text.setText("播放中");
                        }
                    }
                }
                break;
            case R.id.btnStop:
                if (mMediaPlayer != null && !isReleased) {
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.pause();
                }
                break;
        }
    }


}
