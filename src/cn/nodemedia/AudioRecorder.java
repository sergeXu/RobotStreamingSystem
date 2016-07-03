package cn.nodemedia;

/**
 * Created by Serge on 2016/2/19.
 */

import android.media.AudioRecord;
import android.os.Process;
import android.util.Log;

public class AudioRecorder {
    private static final String TAG = "RtmpMedia.AudioRecorder";
    private AudioRecord mAudioRecord = null;
    private AudioRecorder.RecordAudioThread mRecordAudioThread = null;
    private boolean mRecordThreadExitFlag = false;
    private boolean mAudioRecordReleased = true;
    private int mFrameBufferSize;
    private boolean mIsPause = false;

    public AudioRecorder() {
    }

    public int startAudioRecoder(int sampleRate, int channels, int frameSize) {
        this.releaseAudioRecorder();
        this.mFrameBufferSize = frameSize * 2;
        byte channel;
        if (channels == 1) {
            channel = 16;
        } else {
            channel = 12;
        }

        byte samplebit = 2;

        try {
            int minRecordBufSize = AudioRecord.getMinBufferSize(sampleRate, channel, samplebit);
            if (minRecordBufSize < 2048) {
                minRecordBufSize = 2048;
            }

            this.mAudioRecord = new AudioRecord(1, sampleRate, channel, samplebit, minRecordBufSize);
            this.mAudioRecord.startRecording();
            byte[] e = new byte[this.mFrameBufferSize];
            int ret = this.mAudioRecord.read(e, 0, this.mFrameBufferSize);
            if (ret == -3 || ret == -2) {
                throw new Exception();
            }
        } catch (Exception var9) {
            Log.e(TAG, "AudioRecorder 启动失败,可能是权限未开启.");
            return -1;
        }

        if (this.mRecordAudioThread == null) {
            this.mAudioRecordReleased = false;
            this.mRecordThreadExitFlag = false;
            this.mRecordAudioThread = new AudioRecorder.RecordAudioThread();
            this.mRecordAudioThread.start();
        }

        this.mIsPause = false;
        return 0;
    }

    public void pause() {
        this.mIsPause = true;
    }

    public void resume() {
        this.mIsPause = false;
    }

    public void releaseAudioRecorder() {
        if (!this.mAudioRecordReleased) {
            if (this.mRecordAudioThread != null) {
                this.mRecordThreadExitFlag = true;
                this.mRecordAudioThread = null;
            }

            if (this.mAudioRecord != null) {
                this.mAudioRecord.stop();
                this.mAudioRecord.release();
                this.mAudioRecord = null;
            }

            this.mAudioRecordReleased = true;
        }
    }

    class RecordAudioThread extends Thread {
        RecordAudioThread() {
        }

        public void run() {
            try {
                Process.setThreadPriority(-19);
            } catch (Exception var3) {
                Log.e(TAG, "Set record thread priority failed: " + var3.getMessage());
            }

            byte[] data = new byte[AudioRecorder.this.mFrameBufferSize];

            while (!AudioRecorder.this.mRecordThreadExitFlag) {
                int ret = AudioRecorder.this.mAudioRecord.read(data, 0, AudioRecorder.this.mFrameBufferSize);
                if (ret == -3 || ret == -2) {
                    break;
                }

                if (!AudioRecorder.this.mIsPause) {
                    LivePublisher.putAudioData(data, AudioRecorder.this.mFrameBufferSize);
                }
            }

            Object data1 = null;
        }
    }
}

