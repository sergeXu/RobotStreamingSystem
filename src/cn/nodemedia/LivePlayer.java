package cn.nodemedia;

/**
 * Created by Serge on 2016/2/19.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class LivePlayer implements Callback {
    private static LivePlayer sInstance;
    private LivePlayer.LivePlayerDelegate mLivePlayerDelegate = null;
    private AudioManager am = null;
    private static final String TAG = "RtmpMedia.Player";

    static {
        System.loadLibrary("RtmpClient");
    }

    public LivePlayer() {
    }

    public static int init(Context ctx) {
        if (sInstance == null) {
            sInstance = new LivePlayer();
            //sInstance.am = (AudioManager)ctx.getSystemService("audio");
            sInstance.am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            sInstance.am.requestAudioFocus(new OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    Log.i(TAG, "onAudioFocusChange:" + focusChange);
                    if (focusChange == -2) {
                        LivePlayer.sInstance.jniPause();
                    } else if (focusChange == 1) {
                        LivePlayer.sInstance.jniResume();
                    }

                }
            }, 3, 1);
            return sInstance.jniInit(ctx);
        } else {
            return 0;
        }
    }

    public static void setUIVIew(SurfaceView UIView) {
        if (UIView == null) {
            sInstance.jniSetUIVIew((Object) null);
        } else {
            UIView.getHolder().setKeepScreenOn(true);
            UIView.getHolder().addCallback(sInstance);
        }

    }

    public static void setBufferTime(int bufferTime) {
        sInstance.jniSetBufferTime(bufferTime);
    }

    public static void setMaxBufferTime(int maxBufferTime) {
        sInstance.jniSetMaxBufferTime(maxBufferTime);
    }

    public static void startPlay(String rtmpUrl) {
        startPlay(rtmpUrl, "", "");
    }

    public static void startPlay(String rtmpUrl, String pageUrl, String swfUrl) {
        sInstance.jniStartPlay(rtmpUrl, pageUrl, swfUrl);
    }

    public static void stopPlay() {
        sInstance.jniStopPlay();
    }

    public static boolean capturePicture(String savePath) {
        if (sInstance.jniGetVideoWidth() != 0 && sInstance.jniGetVideoHeight() != 0) {
            try {
                File e = new File(savePath);
                FileOutputStream out = new FileOutputStream(e);
                Bitmap bitmap = Bitmap.createBitmap(sInstance.jniGetVideoWidth(), sInstance.jniGetVideoHeight(), Config.RGB_565);
                byte[] imageBuffer = sInstance.jniCapturePicture();
                ByteBuffer bytebuffer = ByteBuffer.wrap(imageBuffer);
                bitmap.copyPixelsFromBuffer(bytebuffer);
                bitmap.compress(CompressFormat.JPEG, 85, out);
                out.flush();
                out.close();
                return true;
            } catch (IOException var6) {
                var6.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public static int getBufferLength() {
        return sInstance.jniGetBufferLength();
    }

    public static void setDelegate(LivePlayer.LivePlayerDelegate delegate) {
        sInstance.mLivePlayerDelegate = delegate;
    }

    private void onEvent(int event, String msg) {
        if (this.mLivePlayerDelegate != null) {
            Log.d(TAG, "event:" + event + "  msg:" + msg);
            this.mLivePlayerDelegate.onEventCallback(event, msg);
        }

    }

    private native int jniInit(Object var1);

    private native int jniSetUIVIew(Object var1);

    private native int jniSetBufferTime(int var1);

    private native int jniSetMaxBufferTime(int var1);

    private native int jniStartPlay(String var1, String var2, String var3);

    private native int jniStopPlay();

    private native int jniGetVideoWidth();

    private native int jniGetVideoHeight();

    private native void jniPause();

    private native void jniResume();

    private native byte[] jniCapturePicture();

    private native int jniGetBufferLength();

    public void surfaceCreated(SurfaceHolder holder) {
        sInstance.jniSetUIVIew(holder.getSurface());
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        sInstance.jniSetUIVIew((Object) null);
    }

    public interface LivePlayerDelegate {
        void onEventCallback(int var1, String var2);
    }
}
