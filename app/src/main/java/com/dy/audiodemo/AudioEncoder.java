package com.dy.audiodemo;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by dy on 2017/3/29 17:06.
 */

public class AudioEncoder {
    private static final String TAG = "AudioEncoder";
    private String encodeType;

    private String dstPath;

    private MediaCodec mediaEncode;
    private MediaExtractor mediaExtractor;

    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;

    private MediaCodec.BufferInfo encodeBufferInfo;
    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private FileInputStream fis;
    private BufferedInputStream bis;
    private int totalSize;

    private ArrayList<byte[]> chunkPCMDataContainer;//PCM数据块容器
    private OnCompleteListener onCompleteListener;
    private OnProgressListener onProgressListener;
    private long fileTotalSize;
    private long decodeSize;


    /**
     * 设置编码器类型
     *
     * @param encodeType
     */
    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }



    public void setPCMData(ArrayList<byte[]> chunkPCMDataContainer, String dstPath) {
        this.chunkPCMDataContainer = chunkPCMDataContainer;
        this.dstPath = dstPath;
        this.totalSize = chunkPCMDataContainer.size();
    }

    /**
     * 此类已经过封装
     * 调用prepare方法 会初始化Decode 、Encode 、输入输出流 等一些列操作
     */
    public void prepare() {

        if (encodeType == null) {
            throw new IllegalArgumentException("encodeType can't be null");
        }


        try {
            fos = new FileOutputStream(new File(dstPath));
            bos = new BufferedOutputStream(fos, 200 * 1024);


        } catch (IOException e) {
            e.printStackTrace();
        }


        if (dstPath == null) {
            throw new IllegalArgumentException("dstPath can't be null");
        }

        if (chunkPCMDataContainer == null) {
            chunkPCMDataContainer = new ArrayList<>();
        }

        //initMediaDecode();//解码器

        if (TextUtils.equals(encodeType, MediaFormat.MIMETYPE_AUDIO_AAC)) {
            initAACMediaEncode();//AAC编码器
        } else if (TextUtils.equals(encodeType, MediaFormat.MIMETYPE_AUDIO_MPEG)) {
            initMPEGMediaEncode();//mp3编码器
        }

    }




    /**
     * 初始化AAC编码器
     */
    private void initAACMediaEncode() {
        try {
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(encodeType, 44100, 2);//参数对应-> mime type、采样率、声道数
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);
            mediaEncode = MediaCodec.createEncoderByType(encodeType);
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaEncode == null) {
            Log.e(TAG, "create mediaEncode failed");
            return;
        }
        mediaEncode.start();
        encodeInputBuffers = mediaEncode.getInputBuffers();
        encodeOutputBuffers = mediaEncode.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * 初始化MPEG编码器
     */
    private void initMPEGMediaEncode() {

    }

    private boolean codeOver = true;

    /**
     * 开始转码
     * 音频数据先解码成PCM  PCM数据在编码成想要得到的{@link #encodeType}音频格式
     * mp3->PCM->aac
     */
    public void startAsync() {
        showLog("start");

        //new Thread(new DecodeRunnable()).start();
        new Thread(new EncodeRunnable()).start();

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
     * 在Container中{@link #chunkPCMDataContainer}取出PCM数据
     *
     * @return PCM数据块
     */
    private byte[] getPCMData() {
        synchronized (AudioEncoder.class) {//记得加锁
            if (onProgressListener != null) {
                float percent = (float) (totalSize - chunkPCMDataContainer.size()) / (float) totalSize;
                onProgressListener.progress((int) (percent * 100));
            }

            showLog("getPCM:" + chunkPCMDataContainer.size());
            if (chunkPCMDataContainer.isEmpty()) {
                return null;
            }

            byte[] pcmChunk = chunkPCMDataContainer.get(0);//每次取出index 0 的数据
            chunkPCMDataContainer.remove(pcmChunk);//取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
            return pcmChunk;
        }
    }




    /**
     * 编码PCM数据 得到{@link #encodeType}格式的音频文件，并保存到{@link #dstPath}
     */
    private void dstAudioFormatFromPCM() {

        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;
        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        byte[] chunkPCM;

//        showLog("doEncode");
        for (int i = 0; i < encodeInputBuffers.length - 1; i++) {

            chunkPCM = getPCMData();//获取解码器所在线程输出的数据 代码后边会贴上
            if (chunkPCM == null) {
                break;
            }
            inputIndex = mediaEncode.dequeueInputBuffer(-1);//同解码器
            inputBuffer = encodeInputBuffers[inputIndex];//同解码器
            inputBuffer.clear();//同解码器
            inputBuffer.limit(chunkPCM.length);
            inputBuffer.put(chunkPCM);//PCM数据填充给inputBuffer
            mediaEncode.queueInputBuffer(inputIndex, 0, chunkPCM.length, 0, 0);//通知编码器 编码
        }

        outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);//同解码器
        while (outputIndex >= 0) {//同解码器

            outBitSize = encodeBufferInfo.size;
            outPacketSize = outBitSize + 7;//7为ADTS头部的大小
            outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
            outputBuffer.position(encodeBufferInfo.offset);
            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
            chunkAudio = new byte[outPacketSize];
            addADTStoPacket(chunkAudio, outPacketSize);//添加ADTS 代码后面会贴上
            outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中 偏移量offset=7 你懂得
            outputBuffer.position(encodeBufferInfo.offset);
//                showLog("outPacketSize:" + outPacketSize + " encodeOutBufferRemain:" + outputBuffer.remaining());
            try {
                bos.write(chunkAudio, 0, chunkAudio.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaEncode.releaseOutputBuffer(outputIndex, false);
            outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);

        }
    }

    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE


// fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
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

        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fos = null;
        }

        if (mediaEncode != null) {
            mediaEncode.stop();
            mediaEncode.release();
            mediaEncode = null;
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
     * 编码线程
     */
    private class EncodeRunnable implements Runnable {

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            while (!codeOver || !chunkPCMDataContainer.isEmpty()) {
                dstAudioFormatFromPCM();
            }
            if (onProgressListener != null) {
                onProgressListener.progress(100);
            }
            if (onCompleteListener != null) {
                onCompleteListener.completed();
            }
            showLog("size:" + fileTotalSize + " decodeSize:" + decodeSize + "time:" + (System.currentTimeMillis() - t));
        }
    }


    /**
     * 转码完成回调接口
     */
    public interface OnCompleteListener {
        void completed();
    }

    /**
     * 转码进度监听器
     */
    public interface OnProgressListener {
        void progress(int progress);
    }


    /**
     * 设置转码完成监听器
     *
     * @param onCompleteListener
     */
    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    private void showLog(String msg) {
        Log.e("AudioEecoder", msg);
    }
}
