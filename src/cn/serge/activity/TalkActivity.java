package cn.serge.activity;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import cn.serge.R;
import cn.serge.adapters.MsgAdapter;
import cn.serge.models.Msg;
import cn.serge.utils.SharedPreUtil;

public class TalkActivity  extends Activity {

	private ListView msgListView;

	private EditText inputText;

	private Button send;
	
	private MsgAdapter adapter;

	private List<Msg> msgList = new ArrayList<Msg>();
	
	private String personSendName,personReceiveName;
	
	private static String ServerIP = "192.168.0.60";
	private static final int ServerPort = 20000;
	
	private Socket socket = null;
	private String strMessage;
	private String msgInfo;
	private boolean isConnect = false;
	private OutputStream outStream;
	private Handler myHandler = null;
	private ReceiveThread receiveThread = null;
	private boolean isReceive = false;
	private long userId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_talk);
		initMsgs();
		adapter = new MsgAdapter(TalkActivity.this, R.layout.msg_item, msgList);
		inputText = (EditText) findViewById(R.id.input_text);
		send = (Button) findViewById(R.id.send);
		msgListView = (ListView) findViewById(R.id.msg_list_view);
		msgListView.setAdapter(adapter);
		ServerIP = (String) SharedPreUtil.get(TalkActivity.this, "edittext_preference_ChattingAddress", "");
		
		if (!isConnect){
			new Thread(connectThread).start();
		}
		userId=Long.parseLong((String)SharedPreUtil.get(TalkActivity.this, "edittext_preference_ClientId", "0"));
		//无存储值
		if(userId<1 )
		{
			userId=Math.round((Math.random()*10000+1000));
			SharedPreUtil.put(TalkActivity.this, "edittext_preference_ClientId", Long.toString(userId));
			String pubAddress =  (String)SharedPreUtil.get(TalkActivity.this, "edittext_preference_pubAddress", "");
			SharedPreUtil.put(TalkActivity.this,"edittext_preference_pubAddress",pubAddress+userId);
		}
		
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String content = inputText.getText().toString();
				if (!"".equals(content)) {
					Msg msg = new Msg(personSendName,content, Msg.TYPE_SENT);
					msgList.add(msg);
					adapter.notifyDataSetChanged();
					msgListView.setSelection(msgList.size());
					inputText.setText("");
					autoReply(content);
					//
					strMessage = content;
					try {
						new Thread(sendThread).start();
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					inputText.setText("");
					
				}
			}
		});
		myHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				//textReceive.append((msg.obj).toString());
				
				//Msg replyMsg = new Msg("Tim",((String)msg.obj), Msg.TYPE_RECEIVED);
				if(((Msg)msg.obj).getId()!=userId)
				{
				Msg replyMsg = new Msg(((Msg)msg.obj).getPersonName(),((Msg)msg.obj).getContent(), Msg.TYPE_RECEIVED);
				msgList.add(replyMsg);
				adapter.notifyDataSetChanged();
				msgListView.setSelection(msgList.size());
				}
			}
		};
	}
	
	private void initMsgs() {
		//personSendName="我";
		personReceiveName="其他人";
		personSendName=(String) SharedPreUtil.get(TalkActivity.this, "edittext_preference_ClientName", "我");
		Msg msg1 = new Msg(personReceiveName,"Hello guy.", Msg.TYPE_RECEIVED);
		msgList.add(msg1);
		Msg msg2 = new Msg(personSendName,"Hello. Who is that?", Msg.TYPE_SENT);
		msgList.add(msg2);
		Msg msg3 = new Msg(personReceiveName,"This is Tom. Nice talking to you. ", Msg.TYPE_RECEIVED);
		msgList.add(msg3);
	}
	private void autoReply(String content){
		//Log.d("talking", "going to autoreply");
		String tempCnt=content.trim().toLowerCase();
		//Log.d("tempCnt", tempCnt);
		String temprep="NULL";
		//自动回复字段
		if(tempCnt.equals("hello"))
		{
			temprep = "hello too";
		}
		else if (tempCnt.equals("bye"))
		{
			temprep = "see you later";
		}
		else if (tempCnt.equals("hi"))
		{
			temprep = "Nice to see you";
		}
		
		Log.d("temprep", temprep);
		
		if(temprep!="NULL")
		{
		//	Log.d("talking", "get something to return *********");
			Msg replyMsg = new Msg(personReceiveName,temprep, Msg.TYPE_RECEIVED);
			msgList.add(replyMsg);
			adapter.notifyDataSetChanged();
			msgListView.setSelection(msgList.size());
		}
		else return;
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
					String isol="#&";
					String endSingal="\n@#$%&\n";
					msgInfo="1111"+isol+userId+isol+personSendName+isol;
					msgInfo+=strMessage;
					msgInfo+=isol;
					msgInfo+=endSingal;
					sendBuffer = msgInfo.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					outStream = socket.getOutputStream();
					outStream.write(sendBuffer);
					outStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
	//接收线程
		private class ReceiveThread extends Thread{
			private InputStream inStream = null;
			
			private byte[] buffer;
			private String str = null;
			
			ReceiveThread(Socket socket){
				try {
					inStream = socket.getInputStream();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			@Override
			public void run(){
				while(isReceive){
					buffer = new byte[1024];
					try {
						inStream.read(buffer);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						//解析数据包
						DataInputStream  dis=new DataInputStream(new ByteArrayInputStream(buffer));
						//int code = dis.readInt();
						//long comingId = dis.readLong();
						//过滤
						//if(comingId==1111)
						{
						
						str = new String(buffer,"UTF-8").trim();
						
						String[] parasites=str.split("#&",5);
						Msg msgStruct= new Msg(parasites[2],parasites[3],Msg.TYPE_RECEIVED,Long.parseLong(parasites[1]));
						
						Message msg = new Message();
						msg.obj = msgStruct;
						
						myHandler.sendMessage(msg);
						}
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		@Override
		protected void onDestroy() {
			// TODO Auto-generated method stub
			super.onDestroy();
			
			if(receiveThread != null){
				isReceive = false;
				receiveThread.interrupt();
			}
		}
}
