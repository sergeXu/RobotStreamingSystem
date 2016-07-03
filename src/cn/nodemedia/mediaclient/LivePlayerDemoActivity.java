package cn.nodemedia.mediaclient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import cn.nodemedia.LivePlayer;
import cn.nodemedia.LivePlayer.LivePlayerDelegate;
import cn.nodemedia.mediaclient.adapters.MsgAdapter;
import cn.nodemedia.mediaclient.models.Msg;
import cn.nodemedia.mediaclient.utils.FileUtil;
import cn.nodemedia.mediaclient.utils.LogUtil;
import cn.nodemedia.mediaclient.utils.SharedPreUtil;

public class LivePlayerDemoActivity extends Activity implements OnClickListener {
    // LinearLayout liner0;
    boolean isPlaying;
    int tsID;
    String outTsPath;
    SurfaceView sv;
    EditText logText;
    EditText edittext_channel;
    TextView text_videoInfo;
    TextView text_channelId;
    TextView text_video_param;
    Button channelBtn;
    Button reFreshBtn;
    Boolean showLog = false, enableVideo = true;
    float srcWidth;
    float srcHeight;
    DisplayMetrics dm;
    String playUrl;
    ImageButton capBtn;
    //talk
    private ListView msgListView;

    private EditText inputText;

    private Button send;

    private MsgAdapter adapter;

    private List<Msg> msgList = new ArrayList<Msg>();

    private String personSendName, personReceiveName;
    private boolean isTalking = false;
    private boolean isIniTalking = false;

    private static String ServerIP = "192.168.0.60";
    private static final int ServerPort = 20000;

    private DatagramSocket dSocket = null;
    private static final int VideoInfoQuestPORT = 35034;
    private DatagramSocket timeSocket = null;
    private static final int TimeTestQuestPORT = 35041;

    private Socket socket = null;
    private String strMessage;
    private String msgInfo;
    private boolean isConnect = false;
    private OutputStream outStream;
    private Handler myHandler = null;
    private ReceiveThread receiveThread = null;
    private boolean isReceive = false;
    private long userId;

    private boolean isLogBufferSize = false;
    private boolean isAutoRecMsg = false;

    private boolean isShowVideoParam = false;
    private boolean isShowVideoInfo = false;
    public static final int MESSAGE_COME = 8;
    public static final int VIDEO_PARAM_UPDATE = 10;
    public static final int VIDEO_INFO_COME = 9;
    public static final int  CONTROL_TIME_INFO_COME = 11;
    public static final int  CONTROL_CONNECT_SUCCESS  = 12;
    public static final int PLAY_SERVER_PORT = 35030;

    private boolean isPublicChating = true;
    private InetAddress chattingServerlocal = null;
    private DatagramSocket chattingMsgSocket = null;
    //for logging
    private static final String TAG = "PlayerActivity";
    //video param
    private StringBuilder videoParam= new StringBuilder("");
    private String paramResolution="";
    private String paramFrameRate="";
    private String paramEncodingFormat="";
    private double paramControlLatency=0;
    private double paramLatency=0;
    private int paramBitRate=0;
    private int paramBaseBitRate=0;
    private String paramChannelId="";
    private double paramBaseEncodingLat=0;
    private boolean isControlTimeTesting =true;
    /*
                        Message mymsg = new Message();
                        mymsg.what = VIDEO_PARAM_UPDATE;
                        myHandler.sendMessage(mymsg);
     */
    //decimal format for display
    DecimalFormat df = new DecimalFormat("#0.000");

    //remote controlPart
    private boolean isRemoteControl;
    private String remoteRobotIp;
    private int remoteRobotPort;
    private Socket remoteControlSocket = null;
    private boolean isRemoteControlConnect = false;
    private String remoteControlMsgInfo;
    private byte[] remoteControlSendBuffer = null;
    private OutputStream remoteControlOutStream;

    private  View remoteControlView;
    private Button toForwardBtn;
    private Button toBackwardBtn;
    private Button toleftBtn;
    private Button torightBtn;
    private Button toStopBtn;
    private Button remoteConnectBtn;
    private Button remoteReleaseConnectBtn;
    private boolean isForward = false;
    private boolean isBackward = false;
    private boolean isLeft = false;
    private boolean isRight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        dm = getResources().getDisplayMetrics();
        //默认关闭log
        showLog = (Boolean) SharedPreUtil.get(this, "checkbox_preference_logShow", false);
        enableVideo = (Boolean) SharedPreUtil.get(this, "checkbox_preference_videoShow", true);
        isLogBufferSize = (Boolean) SharedPreUtil.get(this, "checkbox_preference_logBufferSize", false);

        isShowVideoParam = (Boolean) SharedPreUtil.get(this, "checkbox_preference_videoParamShow", false);

        LivePlayer.init(this);
        LivePlayer.setDelegate(new LivePlayerDelegate() {
            @Override
            public void onEventCallback(int event, String msg) {
                Message message = new Message();
                Bundle b = new Bundle();
                b.putString("msg", msg);
                message.setData(b);
                message.what = event;
                handler.sendMessage(message);
            }
        });

        sv = (SurfaceView) findViewById(R.id.surfaceview1);
        capBtn = (ImageButton) findViewById(R.id.play_cap_button);
        capBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String capFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Live_cap" + System.currentTimeMillis() + ".jpg";
                if (LivePlayer.capturePicture(capFilePath)) {
                    Toast.makeText(LivePlayerDemoActivity.this, "截图保存到 " + capFilePath, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LivePlayerDemoActivity.this, "截图保存失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        logText = (EditText) findViewById(R.id.editText3);
        if (!showLog) {
            logText.setVisibility(View.GONE);
        }

        //remote Control inipart
        isRemoteControl = (Boolean) SharedPreUtil.get(this, "checkbox_preference_isRemoteControl", false);
        if(isRemoteControl)
        {
            remoteRobotIp = (String)SharedPreUtil.get(this, "edittext_preference_RobotIp"," ");
            remoteRobotPort = Integer.valueOf((String)SharedPreUtil.get(this, "edittext_preference_RobotPort","0"));

            remoteControlView = findViewById(R.id.RemoteControlPannel);
            toForwardBtn = (Button)findViewById(R.id.ButtonForward);
            toBackwardBtn = (Button)findViewById(R.id.ButtonBackward);
            toleftBtn = (Button)findViewById(R.id.ButtonLeft);
            torightBtn = (Button)findViewById(R.id.ButtonRight);
            toStopBtn = (Button)findViewById(R.id.ButtoStop);
            remoteConnectBtn = (Button)findViewById(R.id.buttonConnect);
            remoteReleaseConnectBtn = (Button)findViewById(R.id.buttonRelease);

            toStopBtn.setOnClickListener(this);
            remoteConnectBtn.setOnClickListener(this);
            remoteReleaseConnectBtn.setOnClickListener(this);

            toForwardBtn.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        toForwardBtn.setAlpha((float)0.5);
                        if (!isForward) {
                            remoteControlSendMessage("w_down");
                            isForward = true;
                        }
                        else {
                           LogUtil.w(TAG, "is forwarding");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        toForwardBtn.setAlpha((float)1.0);
                        //@android:drawable/ic_input_add);
                        remoteControlSendMessage("w_up");
                        isForward = false ;
                    }
                    return true;
                }

            });
            toBackwardBtn.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        toBackwardBtn.setAlpha((float)0.5);
                        if (!isBackward) {
                            remoteControlSendMessage("s_down");
                            isBackward=true;
                        }
                        else {
                            LogUtil.w(TAG, "is isBackwarding");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        toBackwardBtn.setAlpha((float)1.0);
                        remoteControlSendMessage("s_up");
                        isBackward = false;
                    }
                    return true;
                }

            });
            toleftBtn.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        toleftBtn.setAlpha((float)0.5);
                        if (!isLeft) {
                            remoteControlSendMessage("a_down");
                            isLeft = true;
                        }
                        else {
                            LogUtil.w(TAG, "is Lefting");
                            isLeft=false;
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        toleftBtn.setAlpha((float)1.0);
                        remoteControlSendMessage("a_up");
                    }
                    return true;
                }

            });
            torightBtn.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        torightBtn.setAlpha((float)0.5);
                        if (!isRight) {
                            remoteControlSendMessage("d_down");
                            isRight=true;
                        }
                        else {
                            LogUtil.w(TAG, "is Righring");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        torightBtn.setAlpha((float)1.0);
                        remoteControlSendMessage("d_up");
                        isRight=false;
                    }
                    return true;
                }

            });

 //           new Thread(remoteConnectThread).start();
        }

        //videoParam ini
        text_video_param = (TextView) findViewById(R.id.textView_videoParam);
        if(isShowVideoParam)
        {
            text_video_param.setVisibility(View.VISIBLE);
//            videoParam.append("编码格式：").append(paramEncodingFormat).append("\n");
//            videoParam.append("分辨率：").append(paramResolution).append("\n");
//            videoParam.append("帧率：").append(paramFrameRate).append(" fps\n");
//            videoParam.append("参考延时：").append(df.format(paramLatency)).append(" s\n");
//            videoParam.append("码率数据：").append(paramBitRate).append(" kbps");

            text_video_param.setText(videoParam.toString());
        }
        else
        {text_video_param.setVisibility(View.GONE);}
        //频道相关部件

        edittext_channel = (EditText) findViewById(R.id.editText_ChannelId);
        channelBtn = (Button) findViewById(R.id.button_changeChannel);
        reFreshBtn = (Button) findViewById(R.id.button_refresh);
        edittext_channel.setText("");
        channelBtn.setOnClickListener(this);
        reFreshBtn.setOnClickListener(this);

        //load last channel
        String inputTextLastChannel = FileUtil.load(this);
        if (!TextUtils.isEmpty(inputTextLastChannel)) {
            edittext_channel.setText(inputTextLastChannel);
            edittext_channel.setSelection(edittext_channel.length());
          //  Toast.makeText(this, "Restoring succeeded", Toast.LENGTH_SHORT).show();
        }

        //信息服务器地址
        ServerIP = (String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_ChattingAddress", getString(R.string.ChattingAddress));

        //频道信息相关
        text_videoInfo = (TextView) findViewById(R.id.textView_videoInfo);
        text_channelId = (TextView) findViewById(R.id.textView_channelId);
        text_channelId.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isShowVideoInfo) {
                    isShowVideoInfo = true;
                    text_videoInfo.setText("正在查询频道信息");
                    text_videoInfo.setVisibility(View.VISIBLE);
                    getChannelInfoRunnable getChannelRun = new getChannelInfoRunnable();
                    new Thread(getChannelRun).start();
                } else {
                    isShowVideoInfo = false;
                    text_videoInfo.setText("");
                    text_videoInfo.setVisibility(View.GONE);
                }
            }
        });

        //talking
        initMsgs();
        adapter = new MsgAdapter(LivePlayerDemoActivity.this, R.layout.msg_item, msgList);
        inputText = (EditText) findViewById(R.id.input_text);
        send = (Button) findViewById(R.id.send);
        msgListView = (ListView) findViewById(R.id.msg_list_view);
        msgListView.setAdapter(adapter);
        userId = Long.parseLong((String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_ClientId", "0"));
        //无存储值
        if (userId < 1) {
            userId = Math.round((Math.random() * 10000 + 1000));
            SharedPreUtil.put(LivePlayerDemoActivity.this, "edittext_preference_ClientId", Long.toString(userId));
            String pubAddress = (String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_playAddress", getString(R.string.playAddress));
            SharedPreUtil.put(LivePlayerDemoActivity.this, "edittext_preference_pubAddress", pubAddress + userId);
        }

        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = inputText.getText().toString();
                if (!"".equals(content)) {
                    Msg msg = new Msg(personSendName, content, Msg.TYPE_SENT);
                    msgList.add(msg);
                    adapter.notifyDataSetChanged();
                    msgListView.setSelection(msgList.size());
                    inputText.setText("");
                    autoReply(content);
                    //
                    strMessage = content;
                    new Thread(sendThread).start();
                    inputText.setText("");

                }
            }
        });
        myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case (MESSAGE_COME):
                        //textReceive.append((msg.obj).toString());
                        //Msg replyMsg = new Msg("Tim",((String)msg.obj), Msg.TYPE_RECEIVED);
                        if (((Msg) msg.obj).getId() != userId) {
                            Msg replyMsg = new Msg(((Msg) msg.obj).getPersonName(), ((Msg) msg.obj).getContent(), Msg.TYPE_RECEIVED);
                            msgList.add(replyMsg);
                            adapter.notifyDataSetChanged();
                            msgListView.setSelection(msgList.size());
                        }
                        break;
                    case (VIDEO_INFO_COME):
                        text_videoInfo.setText((String) msg.obj);
                        break;
                    case (VIDEO_PARAM_UPDATE):
                        updateVideoParam();
                        break;
//                    case(CONTROL_TIME_INFO_COME):
//                        paramBitRate
//                        break;
                    case(CONTROL_CONNECT_SUCCESS):
                        Toast.makeText(LivePlayerDemoActivity.this, "Robot connect success", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        inputText.setVisibility(View.INVISIBLE);
        send.setVisibility(View.INVISIBLE);
        msgListView.setVisibility(View.INVISIBLE);
        //auto connect and receive messages
        isAutoRecMsg = (Boolean) SharedPreUtil.get(this, "checkbox_preference_AutoRecMsg", true);
        if(isAutoRecMsg)
        {
            if (!isIniTalking) {
                if (!isConnect) {
                    new Thread(connectThread).start();
                    isConnect = true;
                }
            }
        }

        /**
         * 使用Udp发送播放客户端信息
         */
        if (isPublicChating) {
            sendUdpPlayInfo sends= new sendUdpPlayInfo();
            new Thread(sends).start();
        }
        //param update
        if(isShowVideoParam)
        {
            updateVideoParamThread upParaTh= new updateVideoParamThread();
            new Thread(upParaTh).start();
            getTimeLatencyRunnable getTmLty= new getTimeLatencyRunnable();
            new Thread(getTmLty).start();
        }


        if (enableVideo) {
            LivePlayer.setUIVIew(sv);
        } else {
            LivePlayer.setUIVIew(null);
        }

        /**
         * 设置缓冲区时长，与flash编程时一样，可以设置2个值
         * 第一个bufferTime为从连接成功到开始播放的启动缓冲区长度，越小启动速度越快，最小100毫秒
         * 注意：声音因为没有关键帧，所以这个缓冲区足够马上就可以听到声音，但视频需要等待关键帧后才会开始显示画面。
         * 如果你的服务器支持GOP_cache可以开启来加快画面的出现
         */
        LivePlayer.setBufferTime(Integer.valueOf((String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_playBuffer", "300")));

        /**
         * maxBufferTime为最大缓冲区，当遇到网络抖动，较大的maxBufferTime更加平滑，但延迟也会跟着增加。
         * 这个值关乎延迟的大小。
         */
        LivePlayer.setMaxBufferTime(Integer.valueOf((String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_maxPlayBuffer", "1200")));

        //String playUrl = (String) SharedPreUtil.get(LivePlayerDemoActivity.this, "playUrl", "rtmp://7611.lssplay.aodianyun.com/serge/livestream");
        playUrl = (String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_playAddress", getString(R.string.playAddress));
        SharedPreUtil.put(LivePlayerDemoActivity.this, "edittext_preference_playAddress", playUrl); // 记住上次输入的测试地址，只在demo中用，非SDK方法

        /**
         * 开始播放
         */
        LivePlayer.startPlay(playUrl);

        /**
         * Demo调试用例，每200毫秒获取一次缓冲时长 单位毫秒
         */
        if (isLogBufferSize) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    while (!LivePlayerDemoActivity.this.isFinishing()) {
                        LogUtil.d(TAG, "BufferLength:" + LivePlayer.getBufferLength());
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        //get saved info
        if(savedInstanceState!=null)
        {
            //edittext_channel.setText(savedInstanceState.getString("Channel"));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_changeChannel:
                String channelToChange = edittext_channel.getText().toString();
                playUrl = (String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_playAddress", getString(R.string.playAddress));
                playUrl += channelToChange;
                //fresh功能
                LivePlayer.stopPlay();
                Toast.makeText(LivePlayerDemoActivity.this, "切换频道中。。。", Toast.LENGTH_SHORT).show();
                //LivePlayer.setUIVIew(null);
                //doVideoFix();
                LivePlayer.setUIVIew(sv);
                LivePlayer.startPlay(playUrl);
                doVideoFix();
                //param update
                paramChannelId = channelToChange;
                LogUtil.d("setBaseVideoParam",paramChannelId);
                //  getVideoParam(paramChannelId);
                getChannelInfoRunnable getChannelRun = new getChannelInfoRunnable();
                new Thread(getChannelRun).start();
                break;
            case R.id.button_refresh:
                //初始化聊天后台连接
                if (!isIniTalking) {
                    if (!isConnect) {
                        new Thread(connectThread).start();
                        isConnect = true;
                    }
                }
                if (!isTalking) {
                    inputText.setVisibility(View.VISIBLE);
                    send.setVisibility(View.VISIBLE);
                    msgListView.setVisibility(View.VISIBLE);
                    isTalking = true;
                    //	Toast.makeText(LivePlayerDemoActivity.this, "显示聊天框",Toast.LENGTH_SHORT).show();
                    if(isRemoteControl)
                    this.remoteControlView.setVisibility(View.VISIBLE);
                } else {
                    inputText.setVisibility(View.INVISIBLE);
                    send.setVisibility(View.INVISIBLE);
                    msgListView.setVisibility(View.INVISIBLE);
                    isTalking = false;
                    //	Toast.makeText(LivePlayerDemoActivity.this, "关闭聊天框",Toast.LENGTH_SHORT).show();
                    if(isRemoteControl)
                    this.remoteControlView.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.ButtoStop:
                remoteControlSendMessage("stop");
                Toast.makeText(LivePlayerDemoActivity.this, "Stop order sended", Toast.LENGTH_SHORT).show();
                break;
            case R.id.buttonConnect:
                new Thread(remoteConnectThread).start();
                remoteControlSendMessage("control");
                break;
            case R.id.buttonRelease:
                remoteControlSendMessage("release");
                remoteControlSocket = null;
                isRemoteControlConnect = false;
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //save last channel
        String inputTextLastChannel = edittext_channel.getText().toString();
        FileUtil.save(this,inputTextLastChannel);
        LivePlayer.stopPlay();
        isPublicChating = false;
        isControlTimeTesting =false;
        isShowVideoParam = false;
        if (receiveThread != null) {
            isReceive = false;
            receiveThread.interrupt();
        }
    }
    //for save app state
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
//        String tempChannel= edittext_channel.getText().toString();
//        if(tempChannel!=null&&tempChannel!="")
//        savedInstanceState.putString("Channel",edittext_channel.getText().toString());
//        else savedInstanceState.putString("Channel","");
    }


    /**
     * 监听手机旋转，不销毁activity进行画面旋转，再缩放显示区域
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //隐藏控件
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            edittext_channel.setVisibility(View.GONE);
            reFreshBtn.setVisibility(View.GONE);
            channelBtn.setVisibility(View.GONE);
            text_channelId.setVisibility(View.GONE);

            inputText.setVisibility(View.GONE);
            send.setVisibility(View.GONE);
            msgListView.setVisibility(View.GONE);
            if(text_videoInfo.getVisibility()==View.VISIBLE) {
                text_videoInfo.setVisibility(View.GONE);
            }
            if(isRemoteControl){
                remoteControlView.setVisibility(View.GONE);
            }
//            if(isShowVideoParam)
//                text_video_param.setVisibility(View.GONE);
        } else {
            edittext_channel.setVisibility(View.VISIBLE);
            reFreshBtn.setVisibility(View.VISIBLE);
            channelBtn.setVisibility(View.VISIBLE);
            text_channelId.setVisibility(View.VISIBLE);
            //channel info partion
//            if(isShowVideoParam)
//                text_video_param.setVisibility(View.VISIBLE);
            //talking partion
            if (isTalking) {
                inputText.setVisibility(View.VISIBLE);
                send.setVisibility(View.VISIBLE);
                msgListView.setVisibility(View.VISIBLE);
                if(isRemoteControl){
                    remoteControlView.setVisibility(View.VISIBLE);
                }
            } else {
                inputText.setVisibility(View.INVISIBLE);
                send.setVisibility(View.INVISIBLE);
                msgListView.setVisibility(View.INVISIBLE);
                if(isRemoteControl){
                    remoteControlView.setVisibility(View.INVISIBLE);
                }
            }
        }
        doVideoFix();
    }

    /**
     * 视频画面高宽等比缩放，此SDK——demo 取屏幕高宽做最大高宽缩放
     */
    private void doVideoFix() {
        float maxWidth = dm.widthPixels;
        float maxHeight = dm.heightPixels;
        float fixWidth;
        float fixHeight;
        if (srcWidth / srcHeight <= maxWidth / maxHeight) {
            fixWidth = srcWidth * maxHeight / srcHeight;
            fixHeight = maxHeight;
        } else {
            fixWidth = maxWidth;
            fixHeight = srcHeight * maxWidth / srcWidth;
        }
        ViewGroup.LayoutParams lp = sv.getLayoutParams();
        lp.width = (int) fixWidth;
        lp.height = (int) fixHeight;

        sv.setLayoutParams(lp);
    }


    private Handler handler = new Handler() {
        // 回调处理
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            StringBuffer sb = new StringBuffer();
            SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
            String sRecTime = sDateFormat.format(new java.util.Date());
            sb.append(sRecTime);
            sb.append(" - ");
            sb.append(msg.getData().getString("msg"));
            sb.append("\r\n");
            logText.append(sb);

            switch (msg.what) {
                case 1000:
                    Toast.makeText(LivePlayerDemoActivity.this, "正在连接视频",
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1001:
                    //Toast.makeText(LivePlayerDemoActivity.this, "视频连接成功",
                    // Toast.LENGTH_SHORT).show();
                    break;
                case 1002:
                    Toast.makeText(LivePlayerDemoActivity.this, "视频连接失败",
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1003:
                    //Toast.makeText(LivePlayerDemoActivity.this, "视频开始重连",
                    //LivePlayer.stopPlay();	//自动重连总开关
                    break;
                case 1004:
//				Toast.makeText(LivePlayerDemoActivity.this, "视频播放结束", Toast.LENGTH_SHORT).show();
                    break;
                case 1005:
                    Toast.makeText(LivePlayerDemoActivity.this, "网络异常,播放中断",
                            Toast.LENGTH_SHORT).show();
                    //播放中途网络异常，回调这里。1秒后重连，如不需要，可停止
                    //LivePlayer.stopPlay();
                    break;
                case 1006:
                    Toast.makeText(LivePlayerDemoActivity.this, "视频数据未找到",
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1007:
                    // Toast.makeText(LivePlayerDemoActivity.this, "音频数据未找到",
                    // Toast.LENGTH_SHORT).show();
                    break;
                case 1008:
                    // Toast.makeText(LivePlayerDemoActivity.this, "无法打开视频解码器",
                    // Toast.LENGTH_SHORT).show();
                    break;
                case 1009:
                    // Toast.makeText(LivePlayerDemoActivity.this, "无法打开音频解码器",
                    // Toast.LENGTH_SHORT).show();
                    break;
                case 1100:
                    System.out.println("NetStream.Buffer.Empty");
                    break;
                case 1101:
                    System.out.println("NetStream.Buffer.Buffering");
                    break;
                case 1102:
                    System.out.println("NetStream.Buffer.Full");
                    break;
                case 1103:
                    System.out.println("Stream EOF");
                    //客服端明确收到服务端发送来的 StreamEOF 和 NetStream.Play.UnpublishNotify时回调这里
                    //收到本事件，说明：此流的发布者明确停止了发布，或者网络异常，被服务端明确关闭了流
                    //本sdk仍然会继续在1秒后重连，如不需要，可停止
//				LivePlayer.stopPlay(); 
                    break;
                case 1104:
                    /**
                     * 得到 解码后得到的视频高宽值,可用于重绘surfaceview的大小比例 格式为:{width}x{height}
                     * 本例使用LinearLayout内嵌SurfaceView
                     * LinearLayout的大小为最大高宽,SurfaceView在内部等比缩放,画面不失真
                     * 等比缩放使用在不确定视频源高宽比例的场景,用上层LinearLayout限定了最大高宽
                     */
                    String[] info = msg.getData().getString("msg").split("x");
                    srcWidth = Integer.valueOf(info[0]);
                    srcHeight = Integer.valueOf(info[1]);
                    doVideoFix();
                    break;
                default:
                    break;
            }
        }
    };


    private int updateVideoParam()
    {
        videoParam.delete(0, videoParam.length());
        videoParam.append("编码格式：").append(paramEncodingFormat).append("\n");
        videoParam.append("分辨率：").append(paramResolution).append("\n");
        videoParam.append("帧率：").append(paramFrameRate).append(" fps\n");
        videoParam.append("视频延时：").append(df.format(paramLatency)).append(" s\n");
        videoParam.append("信令延时：").append(df.format(paramControlLatency)).append(" s\n");
        videoParam.append("码率数据：").append(paramBitRate).append(" kbps");

        text_video_param.setText(videoParam.toString());
        LogUtil.d("LivePlayerDemoActivity","updateVideoParam");
        return 1;
    }
    private void initMsgs() {
        //personSendName="我";
        personReceiveName = "其他人";
        personSendName = (String) SharedPreUtil.get(LivePlayerDemoActivity.this, "edittext_preference_ClientName", getString(R.string.ClientName));
        //Msg msg1 = new Msg(personReceiveName,"Hello guy.", Msg.TYPE_RECEIVED);
        //msgList.add(msg1);
        //Msg msg2 = new Msg(personSendName,"Hello. Who is that?", Msg.TYPE_SENT);
        //msgList.add(msg2);
        //Msg msg3 = new Msg(personReceiveName,"This is Tom. Nice talking to you. ", Msg.TYPE_RECEIVED);
        //msgList.add(msg3);
    }

    private void autoReply(String content) {
        //Log.d("talking", "going to autoreply");
        String tempCnt = content.trim().toLowerCase();
        //Log.d("tempCnt", tempCnt);
        String temprep = "NULL";
        //自动回复字段
        if (tempCnt.equals("hello")) {
            temprep = "hello too";
        } else if (tempCnt.equals("bye")) {
            temprep = "see you later";
        } else if (tempCnt.equals("hi")) {
            temprep = "Nice to see you";
        }

        LogUtil.d(TAG, temprep);

        if (temprep != "NULL") {
            //	Log.d("talking", "get something to return *********");
            Msg replyMsg = new Msg(personReceiveName, temprep, Msg.TYPE_RECEIVED);
            msgList.add(replyMsg);
            adapter.notifyDataSetChanged();
            msgListView.setSelection(msgList.size());
        } else return;


    }

    //send udp play info
   class sendUdpPlayInfo implements Runnable {
        private String isol = "%%##";
        //拼装要发送频道信息
        private String msg = new String("4444" + isol + 12345);

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            try {
                byte[] buf = "Hello, I am sender!".getBytes();  //数据
                chattingServerlocal = InetAddress.getByName(ServerIP);
                sb.append("已找到服务器,连接中...");
            } catch (UnknownHostException e) {
                sb.append("未找到服务器.");
                e.printStackTrace();
            }
            try {
                chattingMsgSocket = new DatagramSocket(); // 注意此处要先在配置文件里设置权限,否则会抛权限不足的异常
                sb.append("正在连接服务器...");
            } catch (SocketException e) {
                e.printStackTrace();
                sb.append("服务器连接失败.");
            }
            while (isPublicChating) {
                int msg_len = 0;
                try {
                    msg_len = msg == null ? 0 : msg.getBytes("utf-8").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                DatagramPacket dPacket = new DatagramPacket(msg.getBytes(), msg_len,
                        chattingServerlocal, PLAY_SERVER_PORT);
                try {
                    chattingMsgSocket.send(dPacket);
                    sb.append("消息发送成功!").append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                    sb.append("消息发送失败.").append("\n");
                }
                LogUtil.d(" P_send_message_result", msg + msg_len + "\n" + sb.toString());

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class getTimeLatencyRunnable implements Runnable {
        //分割符
        private String isol = "%%##";
        //拼装要发送频道信息
        private String msgString = "4444";
        long sentTime = 0;
        long receiveTime =0;

        @Override
        public void run() {
            InetAddress local = null;
            byte[] msgRec = new byte[1024];
            try {
                local = InetAddress.getByName(ServerIP);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            try {
                timeSocket = new DatagramSocket(); // 注意此处要先在配置文件里设置权限,否则会抛权限不足的异常
            } catch (SocketException e) {
                e.printStackTrace();
            }
            int msg_len = 0;
           while(isControlTimeTesting) {
               msgString = "4444" + isol + System.currentTimeMillis();
               try {
                   msg_len = msgString == null ? 0 : msgString.getBytes("utf-8").length;
               } catch (UnsupportedEncodingException e) {
                   e.printStackTrace();
               }
               DatagramPacket dPacket = new DatagramPacket(msgString.getBytes(), msg_len,
                       local, TimeTestQuestPORT);
               try {
                   timeSocket.send(dPacket);
               } catch (IOException e) {
                   e.printStackTrace();
               }
               //接收信息

               DatagramPacket dPacketRec = new DatagramPacket(msgRec, msgRec.length);
               try {
                   timeSocket.receive(dPacketRec);
                   String datas = new String(dPacketRec.getData(), 0, dPacketRec.getLength());
                   String datasUtf8 = new String(datas.getBytes("utf-8"));
                   //
                   String[] infos = datasUtf8.split(isol);
                   if(infos.length>=2) {
                       sentTime = Long.parseLong(infos[1]);
                   }
                   else
                   {
                       LogUtil.e(TAG, "time test Info error");
                   }
                   if(sentTime>1000000) {
                       receiveTime=System.currentTimeMillis();
                       paramControlLatency = (receiveTime - sentTime) / 1000.0;
                   }
                   else
                   {
                       receiveTime=0;
                       paramControlLatency = 0;
                       LogUtil.d(TAG, "Return error time ");
                   }
                   LogUtil.d("getTimeLatencyRunnable ",String.valueOf(paramControlLatency));
                   Thread.sleep(500);
               } catch (IOException e) {
                   e.printStackTrace();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
            //end looping
            timeSocket.close();

        }

    }
    class getChannelInfoRunnable implements Runnable {
        //分割符
        private String isol = "%%##";
        private String isol_big = "%####";
        //拼装要发送频道信息
        private String msg = "3333";

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            InetAddress local = null;

            try {
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
                msg_len = msg == null ? 0 : msg.getBytes("utf-8").length;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            DatagramPacket dPacket = new DatagramPacket(msg.getBytes(), msg_len,
                    local, VideoInfoQuestPORT);
            try {
                dSocket.send(dPacket);
                sb.append("消息发送成功!").append("\n");
            } catch (IOException e) {
                e.printStackTrace();
                sb.append("消息发送失败.").append("\n");
            }
            LogUtil.d(TAG, sb.toString());

            //接收信息
            byte[] msgRec = new byte[1024];
            DatagramPacket dPacketRec = new DatagramPacket(msgRec, msgRec.length);
            try {
                dSocket.receive(dPacketRec);
                String datas = new String(dPacketRec.getData(), 0, dPacketRec.getLength());
                String datasUtf8 = new String(datas.getBytes("utf-8"));
                //
                String[] infos = datasUtf8.split(isol_big);
                int num = Integer.parseInt(infos[0]);

                StringBuilder Resualt = new StringBuilder("公开发布的视频频道:\n");
                boolean isFindChannel=false;
                if (num == 0)
                    Resualt.append("无正在发布的视频\n");
                else {
                    for (int i = 1; i < num + 1; i++) {
//										Resualt.append(infos[i]);
                        String[] subInfos = infos[i].split(isol);
//										Resualt.append(subInfos[0]);
                        Resualt.append("ID：");
                        Resualt.append(subInfos[1]);
                        Resualt.append("   频道发布人：");
                        Resualt.append(subInfos[2]);
                        Resualt.append("\n");
                        //
                        if((paramChannelId!="")&&subInfos[1].equals(paramChannelId))
                        {
                            LogUtil.d("setBaseVideoParam",subInfos[3]+subInfos[4]+subInfos[5]);
                            if(subInfos.length>=7) {
                                setBaseVideoParam("H264", subInfos[3], subInfos[4], Double.parseDouble(subInfos[6]), Integer.parseInt(subInfos[5]));
                                isFindChannel = true;
                            }
                            else
                                LogUtil.e(TAG,"param error receiving channelInfo");
                        }
                    }
                }
                //can not find right channel
                if(!isFindChannel)
                    iniBaseVideoParam();
                //UI操作
                Message msg = new Message();
                msg.obj = Resualt.toString();
                //msg.obj = datas;
                msg.what = VIDEO_INFO_COME;
                myHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            dSocket.close();

        }

    }
    //local update threads
    class updateVideoParamThread implements Runnable{

        @Override
        public void run() {
            while(isShowVideoParam)
            {
      //          updateVideoParam();
                //test show parameters
                if(paramBaseBitRate!=0)
                paramBitRate = paramBaseBitRate - paramBaseBitRate*2/5+ (int)(Math.random()*paramBaseBitRate*4/5);
                else
                    paramBitRate=0;
                if(paramBaseEncodingLat!=0)
                    paramLatency = paramBaseEncodingLat + Math.random()*paramBaseEncodingLat*4/5 + paramControlLatency;
                else
                    paramLatency=0;

                //UI操作
                Message msg = new Message();
                //msg.obj = datas;
                msg.what = VIDEO_PARAM_UPDATE;
                myHandler.sendMessage(msg);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void setBaseVideoParam(String format,String resl,String fraRate,double latency,int bitrate)
    {
        paramBaseBitRate=bitrate;
        paramFrameRate = fraRate;
        paramResolution = resl;
        paramBaseEncodingLat = latency;
        paramEncodingFormat=format;
    }
    private void iniBaseVideoParam()
    {
        paramBaseBitRate=0;
        paramBitRate=0;
        paramFrameRate = "";
        paramResolution ="";
        paramLatency = 0;
        paramEncodingFormat="";
        paramBaseEncodingLat=0;
    }
    //连接到服务器的接口
    Runnable connectThread = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                //初始化Scoket，连接到服务器
                socket = new Socket(ServerIP, ServerPort);
                isConnect = true;
                //启动接收线程
                isReceive = true;
                receiveThread = new ReceiveThread(socket);
                receiveThread.start();
                System.out.println("----connected success----");
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("UnknownHostException-->" + e.toString());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("IOException" + e.toString());
            }
        }
    };
    //发送消息的接口
    Runnable sendThread = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            byte[] sendBuffer = null;
            try {
                //发送信息打包
                //分隔符
                String isol = "##%##";
                String endSingal = "\n%%##%%\n";
                msgInfo = "1111" + isol + userId + isol + personSendName + isol;
                msgInfo += strMessage;
                msgInfo += isol;
                msgInfo += endSingal;
                sendBuffer = msgInfo.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                outStream = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                outStream.write(sendBuffer);
                outStream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    //接收线程
    private class ReceiveThread extends Thread {
        private InputStream inStream = null;

        private byte[] buffer;
        private String str = null;

        ReceiveThread(Socket socket) {
            try {
                inStream = socket.getInputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (isReceive) {
                buffer = new byte[512];
                LogUtil.d(TAG, "receive message thread start");
                try {
                    inStream.read(buffer);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    System.exit(0);
                }
                try {
                    //解析数据包
                    //	DataInputStream  dis=new DataInputStream(new ByteArrayInputStream(buffer));
                    //	int code = dis.readInt();
                    //	long comingId = dis.readLong();


                    str = new String(buffer, "UTF-8").trim();
                    LogUtil.d(TAG,"receice_message "+str);
                    String[] parasites = str.split("##%##", 5);
                    Msg msgStruct = new Msg(parasites[2], parasites[3], Msg.TYPE_RECEIVED, Long.parseLong(parasites[1]));

                    Message msg = new Message();
                    msg.obj = msgStruct;
                    msg.what = MESSAGE_COME;
                    myHandler.sendMessage(msg);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    /// remote control part
    Runnable remoteConnectThread = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            //   Log.e(TAG,"connectThread).start");
            try {
                //初始化Scoket，连接到服务器
               remoteControlSocket = new Socket (remoteRobotIp ,remoteRobotPort );
                //          Log.e(TAG,"new Socket");
                isRemoteControlConnect = true;
                //启动接收线程
                //System.out.println("----connected success----");
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                LogUtil.e(TAG, " remote Socket init error");
                // System.out.println("UnknownHostException-->" + e.toString());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //  System.out.println("IOException" + e.toString());
            }
            if(remoteControlSocket != null) {
                LogUtil.e(TAG,"remote Socket init success");
      //          Toast.makeText(LivePlayerDemoActivity.this, "Robot connect success", Toast.LENGTH_SHORT).show();
                Message msg = new Message();
                //msg.obj = datas;
                msg.what = CONTROL_CONNECT_SUCCESS;
                myHandler.sendMessage(msg);
            }
        }
    };

    void remoteControlSendMessage(String s) {
        if(isRemoteControlConnect) {
            remoteControlSendBuffer = null;
            try {
                remoteControlSendBuffer = s.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                remoteControlOutStream = remoteControlSocket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                remoteControlOutStream.write(remoteControlSendBuffer);
                remoteControlOutStream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            LogUtil.e(TAG, "send message before ini");
        }
    }



}
