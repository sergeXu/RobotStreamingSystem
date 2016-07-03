package cn.nodemedia.mediaclient;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import cn.serge.R;
import cn.nodemedia.LivePublisher;
import cn.nodemedia.LivePublisher.LivePublishDelegate;
import cn.serge.utils.LogUtil;
import cn.serge.utils.SharedPreUtil;

public class LivePublisherDemoActivity extends Activity implements OnClickListener, LivePublishDelegate {
    private SurfaceView sv;
    private Button micBtn, swtBtn, videoBtn, flashBtn, camBtn;
    private TextView pubIdText;

    private boolean isStarting = false;
    private boolean isMicOn = true;
    private boolean isCamOn = true;
    private boolean isFlsOn = true;
    private boolean isLowLatencyMode = false;

    private ImageButton capBtn;
    private long pubId = 0;
    private boolean isPublicPub = true;

    private String userName = null;
    private DatagramSocket dSocket = null;
    private String ServerIP = null;
    private static final int PUPINFO_SERVER_PORT = 35032;

    //video param
    private String videoResolution;
    private String videoBitRate;
    private String videoFrameRate;
    private String videoEncodingLat;
    //for logging
    private static final String TAG = "PublisherActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encoder);
        isStarting = false;
        sv = (SurfaceView) findViewById(R.id.cameraView);
        micBtn = (Button) findViewById(R.id.button_mic);
        swtBtn = (Button) findViewById(R.id.button_sw);
        videoBtn = (Button) findViewById(R.id.button_video);
        flashBtn = (Button) findViewById(R.id.button_flash);
        camBtn = (Button) findViewById(R.id.button_cam);
        capBtn = (ImageButton) findViewById(R.id.pub_cap_button);
        pubIdText = (TextView) findViewById(R.id.pub_textView_pubId);

        micBtn.setOnClickListener(this);
        swtBtn.setOnClickListener(this);
        videoBtn.setOnClickListener(this);
        flashBtn.setOnClickListener(this);
        camBtn.setOnClickListener(this);
        capBtn.setOnClickListener(this);

        LivePublisher.init(this); // 1.初始化
        LivePublisher.setDelegate(this); // 2.设置事件回调

        //pubid 获取与显示
        pubId = Long.parseLong((String) SharedPreUtil.get(this, "edittext_preference_ClientId", "0"));
        //无存储值,第一次进入功能界面，初始化ID,以及发布地址
        if (pubId < 1) {
            pubId = Math.round((Math.random() * 10000 + 1000));
            SharedPreUtil.put(LivePublisherDemoActivity.this, "edittext_preference_ClientId", Long.toString(pubId));

            String pubAddress = (String) SharedPreUtil.get(LivePublisherDemoActivity.this, "edittext_preference_playAddress", getString(R.string.playAddress));
            SharedPreUtil.put(LivePublisherDemoActivity.this, "edittext_preference_pubAddress", pubAddress + pubId);
        }
        //pubIdText.setText(String.valueOf(pubId));
        pubIdText.setText("Pub Id: " + String.valueOf(pubId));
        //获取用户名
        userName = (String) SharedPreUtil.get(LivePublisherDemoActivity.this, "edittext_preference_ClientName", getString(R.string.ClientName));

        isLowLatencyMode = (Boolean) SharedPreUtil.get(this, "checkbox_preference_LowLatencyMode", false);
        if (!isLowLatencyMode) {

            /**
             * 设置输出音频参数 码率 32kbps 使用HE-AAC ,部分服务端不支持HE-AAC,会导致发布失败
             */
            LivePublisher.setAudioParam(32 * 1000, LivePublisher.AAC_PROFILE_HE);


            /**
             * 设置输出视频参数 宽 640 高 360 fps 15 码率 300kbps 以下建议分辨率及比特率 不用超过1280x720
             * 320X180@15 ~~ 200kbps 480X272@15 ~~ 250kbps 568x320@15 ~~ 300kbps
             * 640X360@15 ~~ 400kbps 720x405@15 ~~ 500kbps 854x480@15 ~~ 600kbps
             * 960x540@15 ~~ 700kbps 1024x576@15 ~~ 800kbps 1280x720@15 ~~ 1000kbps
             * 使用main profile
             */
            LivePublisher.setVideoParam(1280, 720, 15, 1000 * 1000, LivePublisher.AVC_PROFILE_MAIN);
            videoResolution="1280x720";
            videoFrameRate="15";
            videoBitRate="1000" ;
            videoEncodingLat="1.0";
            /**
             * 是否开启背景噪音抑制
             */
            LivePublisher.setDenoiseEnable(true);
        } else {
            //Log.d(TAG, "lowLatency Mode strat");
            LivePublisher.setAudioParam(16 * 1000, LivePublisher.AAC_PROFILE_HE);
            LivePublisher.setVideoParam(480, 272, 15, 250 * 1000, LivePublisher.AVC_PROFILE_MAIN);
            LivePublisher.setDenoiseEnable(false);
            videoResolution="480x272";
            videoFrameRate="15";
            videoBitRate="250" ;
            videoEncodingLat="0.5";

        }
        /**
         * 开始视频预览，
         * cameraPreview ： 用以回显摄像头预览的SurfaceViewd对象，如果此参数传入null，则只发布音频
         * interfaceOrientation ： 程序界面的方向，也做调整摄像头旋转度数的参数，
         * camId： 摄像头初始id，LivePublisher.CAMERA_BACK 后置，LivePublisher.CAMERA_FRONE 前置
         */
        LivePublisher.startPreview(sv, getWindowManager().getDefaultDisplay().getRotation(), LivePublisher.CAMERA_BACK); // 5.开始预览 如果传null 则只发布音频

        /**
         * 使用Udp发送直播频道信息
         */
        if (isPublicPub) {
            //信息服务器地址
            ServerIP = (String) SharedPreUtil.get(LivePublisherDemoActivity.this, "edittext_preference_ChattingAddress", getString(R.string.ChattingAddress));
            new Thread(new Runnable() {
                private String isol = "%%##";
                //拼装要发送频道信息
                private String msgToSend = new String("2222" + isol + pubId + isol + userName + isol +videoResolution +isol +videoFrameRate + isol +videoBitRate+isol+videoEncodingLat);

                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    InetAddress local = null;
                    while (isPublicPub) {
                        if (isStarting) {
                            try {
                                byte[] buf = "Hello, I am sender!".getBytes();  //数据
                                local = InetAddress.getByName(ServerIP); // 本机测试
                                sb.append("已找到服务器,连接中...").append("\n");
                            } catch (UnknownHostException e) {
                                sb.append("未找到服务器.").append("\n");
                                e.printStackTrace();
                            }
                            try {
                                dSocket = new DatagramSocket(); // 注意此处要先在配置文件里设置权限,否则会抛权限不足的异常
                                sb.append("正在连接服务器...").append("\n");
                            } catch (SocketException e) {
                                e.printStackTrace();
                                sb.append("服务器连接失败.").append("\n");
                            }
                            int msg_len = 0;
                            try {
                                msg_len = msgToSend == null ? 0 : msgToSend.getBytes("utf-8").length;
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            DatagramPacket dPacket = new DatagramPacket(msgToSend.getBytes(), msg_len,
                                    local, PUPINFO_SERVER_PORT);
                            try {
                                dSocket.send(dPacket);
                                sb.append("消息发送成功!").append("\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                                sb.append("消息发送失败.").append("\n");
                            }
                            LogUtil.d(" P_send_message_result", msgToSend + msg_len + "\n" + sb.toString());
                        }
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 注意：如果你的业务方案需求只做单一方向的视频直播，可以不处理这段

        // 如果程序UI没有锁定屏幕方向，旋转手机后，请把新的界面方向传入，以调整摄像头预览方向
        LivePublisher.setCameraOrientation(getWindowManager().getDefaultDisplay().getRotation());

        // 还没有开始发布视频的时候，可以跟随界面旋转的方向设置视频与当前界面方向一致，但一经开始发布视频，是不能修改视频发布方向的了
        // 请注意：如果视频发布过程中旋转了界面，停止发布，再开始发布，是不会触发"onConfigurationChanged"进入这个参数设置的
        if (!isStarting) {
            switch (getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_0:
                    LivePublisher.setVideoOrientation(LivePublisher.VIDEO_ORI_PORTRAIT);
                    break;
                case Surface.ROTATION_90:
                    LivePublisher.setVideoOrientation(LivePublisher.VIDEO_ORI_LANDSCAPE);
                    break;
                case Surface.ROTATION_180:
                    LivePublisher.setVideoOrientation(LivePublisher.VIDEO_ORI_PORTRAIT_REVERSE);
                    break;
                case Surface.ROTATION_270:
                    LivePublisher.setVideoOrientation(LivePublisher.VIDEO_ORI_LANDSCAPE_REVERSE);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPublicPub = false;
        LivePublisher.stopPreview();
        LivePublisher.stopPublish();

    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.pub_cap_button:
                String capFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/pub_cap" + System.currentTimeMillis() + ".jpg";
                //String capFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Live_cap"+System.currentTimeMillis()+".jpg";
                if (LivePublisher.capturePicture(capFilePath)) {
                    Toast.makeText(LivePublisherDemoActivity.this, "截图保存到 " + capFilePath, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LivePublisherDemoActivity.this, "截图保存失败", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.button_mic:
                if (isStarting) {
                    isMicOn = !isMicOn;
                    LivePublisher.setMicEnable(isMicOn); // 设置是否打开麦克风
                    if (isMicOn) {
                        handler.sendEmptyMessage(2101);
                    } else {
                        handler.sendEmptyMessage(2100);
                    }
                }
                break;
            case R.id.button_sw:
                LivePublisher.switchCamera();// 切换前后摄像头
                LivePublisher.setFlashEnable(false);// 关闭闪光灯,前置不支持闪光灯
                isFlsOn = false;
                flashBtn.setBackgroundResource(R.drawable.ic_flash_off);
                break;
            case R.id.button_video:
                if (isStarting) {
                    LivePublisher.stopPublish();// 停止发布
                } else {
                    /**
                     * 设置视频发布的方向，此方法为可选，如果不调用，则输出视频方向跟随界面方向，如果特定指出视频方向，
                     * 在startPublish前调用设置 videoOrientation ： 视频方向 VIDEO_ORI_PORTRAIT
                     * home键在 下 的 9:16 竖屏方向 VIDEO_ORI_LANDSCAPE home键在 右 的 16:9 横屏方向
                     * VIDEO_ORI_PORTRAIT_REVERSE home键在 上 的 9:16 竖屏方向
                     * VIDEO_ORI_LANDSCAPE_REVERSE home键在 左 的 16:9 横屏方向
                     */
                    // LivePublisher.setVideoOrientation(LivePublisher.VIDEO_ORI_PORTRAIT);

                    /**
                     * 开始视频发布 rtmpUrl rtmp流地址
                     */
                    LivePublisher.startPublish((String) SharedPreUtil.get(LivePublisherDemoActivity.this, "edittext_preference_pubAddress", getString(R.string.pubAddress)));
//				LivePublisher.startPublish(pubUrl);
                }
                break;
            case R.id.button_flash:
                int ret = -1;
                if (isFlsOn) {
                    ret = LivePublisher.setFlashEnable(false);
                } else {
                    ret = LivePublisher.setFlashEnable(true);
                }
                if (ret == -1) {
                    // 无闪光灯,或处于前置摄像头,不支持闪光灯操作
                } else if (ret == 0) {
                    // 闪光灯被关闭
                    flashBtn.setBackgroundResource(R.drawable.ic_flash_off);
                    isFlsOn = false;
                } else {
                    // 闪光灯被打开
                    flashBtn.setBackgroundResource(R.drawable.ic_flash_on);
                    isFlsOn = true;
                }
                break;
            case R.id.button_cam:
                if (isStarting) {
                    isCamOn = !isCamOn;
                    LivePublisher.setCamEnable(isCamOn);
                    if (isCamOn) {
                        handler.sendEmptyMessage(2103);
                    } else {
                        handler.sendEmptyMessage(2102);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onEventCallback(int event, String msg) {
        handler.sendEmptyMessage(event);
    }

    private Handler handler = new Handler() {
        // 回调处理
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 2000:
                    Toast.makeText(LivePublisherDemoActivity.this, "正在发布视频", Toast.LENGTH_SHORT).show();
                    break;
                case 2001:
                    Toast.makeText(LivePublisherDemoActivity.this, "视频发布成功", Toast.LENGTH_SHORT).show();
                    videoBtn.setBackgroundResource(R.drawable.ic_video_start);
                    isStarting = true;
                    break;
                case 2002:
                    Toast.makeText(LivePublisherDemoActivity.this, "视频发布失败", Toast.LENGTH_SHORT).show();
                    break;
                case 2004:
                    Toast.makeText(LivePublisherDemoActivity.this, "视频发布结束", Toast.LENGTH_SHORT).show();
                    videoBtn.setBackgroundResource(R.drawable.ic_video_stop);
                    isStarting = false;
                    break;
                case 2005:
                    Toast.makeText(LivePublisherDemoActivity.this, "网络异常,发布中断", Toast.LENGTH_SHORT).show();
                    break;
                case 2100:
                    // mic off
                    micBtn.setBackgroundResource(R.drawable.ic_mic_off);
                    Toast.makeText(LivePublisherDemoActivity.this, "麦克风静音", Toast.LENGTH_SHORT).show();
                    break;
                case 2101:
                    // mic on
                    micBtn.setBackgroundResource(R.drawable.ic_mic_on);
                    Toast.makeText(LivePublisherDemoActivity.this, "麦克风恢复", Toast.LENGTH_SHORT).show();
                    break;
                case 2102:
                    // camera off
                    camBtn.setBackgroundResource(R.drawable.ic_cam_off);
                    Toast.makeText(LivePublisherDemoActivity.this, "摄像头传输关闭", Toast.LENGTH_SHORT).show();
                    break;
                case 2103:
                    // camera on
                    camBtn.setBackgroundResource(R.drawable.ic_cam_on);
                    Toast.makeText(LivePublisherDemoActivity.this, "摄像头传输打开", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

}
