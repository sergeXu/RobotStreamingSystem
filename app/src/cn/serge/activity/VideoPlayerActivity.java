package cn.serge.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.Toast;

import com.asha.vrlib.MDVRLibrary;
import cn.nodemedia.mediaclient.R;
import cn.serge.adapters.MediaPlayerWrapper;
import cn.serge.utils.SharedPreUtil;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by hzqiujiadi on 16/4/5.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class VideoPlayerActivity extends MD360PlayerActivity {

    private MediaPlayerWrapper mMediaPlayerWrapper = new MediaPlayerWrapper();

    private boolean isLooping = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPlayerWrapper.init();
        mMediaPlayerWrapper.setPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                cancelBusy();
            }
        });
        mMediaPlayerWrapper.getPlayer().setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
                getVRLibrary().onTextureResize(width, height);
            }
        });
        //循环播放设置
        isLooping = (Boolean) SharedPreUtil.get(this, "checkbox_preference_vrLooping", true);
        mMediaPlayerWrapper.setLooping(isLooping);
        Uri uri = getUri();
        if (uri != null){
            mMediaPlayerWrapper.openRemoteFile(uri.toString());
            mMediaPlayerWrapper.prepare();
        }
    }

    @Override
    protected MDVRLibrary createVRLibrary() {
        //设置初始模式
        return MDVRLibrary.with(this)
                .displayMode(MDVRLibrary.DISPLAY_MODE_GLASS)
                .interactiveMode(MDVRLibrary.INTERACTIVE_MODE_MOTION)
                .asVideo(new MDVRLibrary.IOnSurfaceReadyCallback() {
                    @Override
                    public void onSurfaceReady(Surface surface) {
                        mMediaPlayerWrapper.getPlayer().setSurface(surface);
                    }
                })
                .ifNotSupport(new MDVRLibrary.INotSupportCallback() {
                    @Override
                    public void onNotSupport(int mode) {
                        String tip = mode == MDVRLibrary.INTERACTIVE_MODE_MOTION
                                ? "onNotSupport:MOTION" : "onNotSupport:" + String.valueOf(mode);
                        Toast.makeText(VideoPlayerActivity.this, tip, Toast.LENGTH_SHORT).show();
                    }
                })
                .pinchEnabled(true)
                .gesture(new MDVRLibrary.IGestureListener() {
                    @Override
                    public void onClick(MotionEvent e) {
                        Toast.makeText(VideoPlayerActivity.this, "onClick!", Toast.LENGTH_SHORT).show();
                    }
                })
                .build(R.id.surface_view);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayerWrapper.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMediaPlayerWrapper.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaPlayerWrapper.onResume();
    }
}
