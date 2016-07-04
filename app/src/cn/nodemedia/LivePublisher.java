package cn.nodemedia;

/**
 * Created by Serge on 2016/2/19.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.Bitmap.CompressFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.nodemedia.AudioRecorder;

public class LivePublisher {
    private static final String TAG = "RtmpMedia.LivePublisher";
    private static LivePublisher sInstance;
    private LivePublisher.LivePublishDelegate mLivePublishDelegate = null;
    private AudioRecorder mAudioRecorder = null;
    private cn.nodemedia.VideoRecorder mVideoRecorder = null;
    private AudioManager am = null;
    public static final int AAC_PROFILE_LC = 0;
    public static final int AAC_PROFILE_HE = 1;
    public static final int AVC_PROFILE_BASELINE = 0;
    public static final int AVC_PROFILE_MAIN = 1;
    public static final int CAMERA_BACK = 0;
    public static final int CAMERA_FRONT = 1;
    public static final int VIDEO_ORI_PORTRAIT = 0;
    public static final int VIDEO_ORI_LANDSCAPE = 1;
    public static final int VIDEO_ORI_PORTRAIT_REVERSE = 2;
    public static final int VIDEO_ORI_LANDSCAPE_REVERSE = 3;
    private int mCamId = 0;
    private int mOri = 0;
    private boolean isStartPreview = false;

    static {
        System.loadLibrary("RtmpClient");
    }

    public LivePublisher() {
    }

    public static int init(Context ctx) {
        if (sInstance == null) {
            sInstance = new LivePublisher();
            sInstance.mAudioRecorder = new AudioRecorder();
            sInstance.mVideoRecorder = new VideoRecorder();
            sInstance.am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            sInstance.am.requestAudioFocus(new OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    Log.i(TAG, "onAudioFocusChange:" + focusChange);
                    if (focusChange == -2) {
                        LivePublisher.sInstance.mAudioRecorder.pause();
                        LivePublisher.sInstance.mVideoRecorder.pause();
                    } else if (focusChange == 1) {
                        (new Handler()).postDelayed(new Runnable() {
                            public void run() {
                                LivePublisher.sInstance.mAudioRecorder.resume();
                                LivePublisher.sInstance.mVideoRecorder.resume();
                            }
                        }, 500L);
                    }

                }
            }, 3, 1);
            return sInstance.jniInit(ctx);
        } else {
            return 0;
        }
    }

    public static int startPreview(SurfaceView cameraPreview, int interfaceOrientation, int camId) {
        byte ret = 0;
        sInstance.mCamId = camId;
        sInstance.mOri = interfaceOrientation;
        int audioRet = sInstance.mAudioRecorder.startAudioRecoder('걄', 1, 1024);
        int videoRet = sInstance.mVideoRecorder.startVideoRecorder(cameraPreview, interfaceOrientation, camId);
        if (audioRet == -1 && videoRet == -1) {
            ret = -1;
            Log.e(TAG, "Microphone and Camera cannot be open. preview Error.");
        } else if (audioRet == -1) {
            Log.w(TAG, "Microphone cannot be open.");
            setAudioParam(0, 0);
        } else if (videoRet == -1) {
            Log.w(TAG, "Camera cannot be open.");
            setVideoParam(0, 0, 0, 0, 0);
        } else if (videoRet == 0) {
            setCameraParm(sInstance.mVideoRecorder.preWidth, sInstance.mVideoRecorder.preHeight, camId);
            setVideoOrientation(interfaceOrientation);
            sInstance.isStartPreview = true;
        }

        return ret;
    }

    public static int stopPreview() {
        sInstance.isStartPreview = false;
        sInstance.mVideoRecorder.stopVideoRecorder();
        sInstance.mAudioRecorder.releaseAudioRecorder();
        return 0;
    }

    public static int switchCamera() {
        int ret = sInstance.mVideoRecorder.switchCamera();
        sInstance.mCamId = ret;
        if (ret != -1) {
            setCameraParm(sInstance.mVideoRecorder.preWidth, sInstance.mVideoRecorder.preHeight, ret);
        }

        return ret;
    }

    public static int setFlashEnable(boolean flashEnable) {
        return sInstance.mVideoRecorder.setFlashEnable(flashEnable);
    }

    public static int setCameraOrientation(int interfaceOrientation) {
        sInstance.mOri = interfaceOrientation;
        return sInstance.mVideoRecorder.setCameraOrientation(interfaceOrientation);
    }

    public static int startPublish(String rtmpUrl) {
        return startPublish(rtmpUrl, "", "");
    }

    public static int startPublish(String rtmpUrl, String pageUrl, String swfUrl) {
        return jniStartPublish(rtmpUrl, pageUrl, swfUrl);
    }

    public static boolean capturePicture(String savePath) {
        if (!sInstance.isStartPreview) {
            return false;
        } else {
            try {
                File e = new File(savePath);
                FileOutputStream out = new FileOutputStream(e);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] yuvBuffer = sInstance.mVideoRecorder.currentBuffer();
                int w = sInstance.mVideoRecorder.preWidth;
                int h = sInstance.mVideoRecorder.preHeight;
                YuvImage yuvImage = new YuvImage(yuvBuffer, 17, w, h, (int[]) null);
                yuvImage.compressToJpeg(new Rect(0, 0, w, h), 100, bos);
                short rotate = 0;
                switch (sInstance.mOri) {
                    case 0:
                        rotate = 90;
                        break;
                    case 1:
                        rotate = 0;
                        break;
                    case 2:
                        rotate = 270;
                        break;
                    case 3:
                        rotate = 180;
                }

                if (sInstance.mCamId == 1) {
                    if (rotate == 90) {
                        rotate = 270;
                    } else if (rotate == 270) {
                        rotate = 90;
                    }
                }

                byte[] bytes = bos.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Matrix matrix = new Matrix();
                matrix.postRotate((float) rotate);
                Bitmap newBit = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                newBit.compress(CompressFormat.JPEG, 85, out);
                out.flush();
                out.close();
                return true;
            } catch (IOException var13) {
                var13.printStackTrace();
                return false;
            }
        }
    }

    public static void setDelegate(LivePublisher.LivePublishDelegate delegate) {
        sInstance.mLivePublishDelegate = delegate;
    }

    private void onEvent(int event, String msg) {
        if (this.mLivePublishDelegate != null) {
            Log.d(TAG, "event:" + event + "  msg:" + msg);
            this.mLivePublishDelegate.onEventCallback(event, msg);
        }

    }

    private native int jniInit(Object var1);

    public static native int setAudioParam(int var0, int var1);

    public static native int setVideoParam(int var0, int var1, int var2, int var3, int var4);

    private static native int setCameraParm(int var0, int var1, int var2);

    public static native int setVideoOrientation(int var0);

    private static native int jniStartPublish(String var0, String var1, String var2);

    public static native int stopPublish();

    public static native int setDenoiseEnable(boolean var0);

    public static native int setMicEnable(boolean var0);

    public static native int setCamEnable(boolean var0);

    public static native int putVideoData(byte[] var0, int var1);

    public static native int putAudioData(byte[] var0, int var1);

    public interface LivePublishDelegate {
        void onEventCallback(int var1, String var2);
    }
}
