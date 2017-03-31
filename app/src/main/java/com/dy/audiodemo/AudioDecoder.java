package com.dy.audiodemo;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 类名 AudioDecoder
 * 作者 dy
 * 功能
 * 创建日期 2017/3/31 14:49
 * 修改日期 2017/3/31 14:49
 */


public class AudioDecoder {
    private static final String TAG = "AudioDecoder";
    private String encodeType;
    private String srcPath;

    private MediaCodec mediaDecode;

    private MediaExtractor mediaExtractor;
    private ByteBuffer[] decodeInputBuffers;
    private ByteBuffer[] decodeOutputBuffers;

    private MediaCodec.BufferInfo decodeBufferInfo;

    private BufferedOutputStream bos;
    private FileInputStream fis;
    private BufferedInputStream bis;
    private int totalSize;

    private ArrayList<byte[]> chunkPCMDataContainer;//PCM数据块容器
    private AudioEncoder.OnCompleteListener onCompleteListener;
    private AudioEncoder.OnProgressListener onProgressListener;
    private long fileTotalSize;
    private long decodeSize;


    /**
     * 设置编码器类型
     *
     * @param encodeType 格式类型
     */
    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }


    public void decode(String encodeType, String srcPath) {
        prepare();
        startAsync();
    }


    /**
     * 此类已经过封装
     * 调用prepare方法 会初始化Decode 、Encode 、输入输出流 等一些列操作
     */
    private void prepare() {

        if (encodeType == null) {
            throw new IllegalArgumentException("encodeType can't be null");
        }

        if (!TextUtils.isEmpty(srcPath)) {
            File file = new File(srcPath);
            fileTotalSize = file.length();
        } else {
            throw new IllegalArgumentException("srcPath can't be null");

        }


        if (chunkPCMDataContainer == null) {
            chunkPCMDataContainer = new ArrayList<>();
        }

        initMediaDecode();//解码器

    }

    /**
     * 初始化解码器
     */
    private void initMediaDecode() {
        try {

            //此类可分离视频文件的音轨和视频轨道
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(srcPath);

            //遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {

                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);

                //获取音频轨道
                if (mime.startsWith("audio")) {
//                  format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                    mediaExtractor.selectTrack(i);//选择此音频轨道
                    mediaDecode = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                    mediaDecode.configure(format, null, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaDecode == null) {
            Log.e(TAG, "create mediaDecode failed");
            return;
        }
        //启动MediaCodec ，等待传入数据

        mediaDecode.start();

        //MediaCodec在此ByteBuffer[]中获取输入数据
        decodeInputBuffers = mediaDecode.getInputBuffers();

        //MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        decodeOutputBuffers = mediaDecode.getOutputBuffers();

        //用于描述解码得到的byte[]数据的相关信息
        decodeBufferInfo = new MediaCodec.BufferInfo();

        showLog("buffers:" + decodeInputBuffers.length);
    }


    private boolean codeOver;

    /**
     * 开始转码
     * 音频数据{@link #srcPath}先解码成PCM  PCM数据在编码成想要得到的{@link #encodeType}音频格式
     * mp3->PCM->aac
     */
    private void startAsync() {
        showLog("start");

        new Thread(new DecodeRunnable()).start();

    }

    /**
     * 将PCM数据存入{@link #chunkPCMDataContainer}
     *
     * @param pcmChunk PCM数据块
     */
    private void putPCMData(byte[] pcmChunk) {
        synchronized (AudioEncoder.class) {//记得加锁
            chunkPCMDataContainer.add(pcmChunk);
        }
    }

    /**
     * 解码{@link #srcPath}音频文件 得到PCM数据块
     *
     * @return 是否解码完所有数据
     */
    private void srcAudioFormatToPCM() {
        for (int i = 0; i < decodeInputBuffers.length - 1; i++) {
            //获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
            int inputIndex = mediaDecode.dequeueInputBuffer(-1);
            if (inputIndex < 0) {
                codeOver = true;
                return;
            }

            //拿到inputBuffer
            ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];

            //清空之前传入inputBuffer内的数据
            inputBuffer.clear();

            //MediaExtractor读取数据到inputBuffer中
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);

            //小于0 代表所有数据已读取完成
            if (sampleSize < 0) {
                codeOver = true;
            } else {
                //通知MediaDecode解码刚刚传入的数据
                mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);

                //MediaExtractor移动到下一取样处
                mediaExtractor.advance();
                decodeSize += sampleSize;
            }
        }

        //获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
        //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
        int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);


        ByteBuffer outputBuffer;
        byte[] chunkPCM;

        //每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
        while (outputIndex >= 0) {

            //拿到用于存放PCM数据的Buffer
            outputBuffer = decodeOutputBuffers[outputIndex];

            //BufferInfo内定义了此数据块的大小
            chunkPCM = new byte[decodeBufferInfo.size];

            //将Buffer内的数据取出到字节数组中
            outputBuffer.get(chunkPCM);

            //数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
            outputBuffer.clear();

            //自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码
            putPCMData(chunkPCM);

            //此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
            mediaDecode.releaseOutputBuffer(outputIndex, false);

            //再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
            outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);
        }

    }


    /**
     * 释放资源
     */
    public void release() {
        try {
            if (bos != null) {
                bos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bos = null;
                }
            }
        }


        if (mediaDecode != null) {
            mediaDecode.stop();
            mediaDecode.release();
            mediaDecode = null;
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }

        if (onCompleteListener != null) {
            onCompleteListener = null;
        }

        if (onProgressListener != null) {
            onProgressListener = null;
        }
        showLog("release");
    }

    /**
     * 解码线程
     */
    private class DecodeRunnable implements Runnable {

        @Override
        public void run() {
            while (!codeOver) {
                srcAudioFormatToPCM();
            }
        }
    }


    /**
     * 解码完成回调接口
     */
    public interface OnCompleteListener {
        void completed();
    }

    /**
     * 解码进度监听器
     */
    public interface OnProgressListener {
        void progress(int progress);
    }


    /**
     * 设置解码完成监听器
     *
     * @param onCompleteListener
     */
    public void setOnCompleteListener(AudioEncoder.OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setOnProgressListener(AudioEncoder.OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    private void showLog(String msg) {
        Log.e("AudioDecoder", msg);
    }
}
