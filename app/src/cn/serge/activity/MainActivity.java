package cn.serge.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import cn.nodemedia.mediaclient.LivePlayerDemoActivity;
import cn.nodemedia.mediaclient.LivePublisherDemoActivity;
import cn.nodemedia.mediaclient.R;

public class MainActivity extends Activity implements OnClickListener {
    private Button playerBtn, toTalkBtn, webIndexBtn;
    private ImageButton settingBtn, exitBtn, infoShowBtn;
    private static final int menu_setting = 1;
    private static final int menu_toVrActivity = 2;
    private static final String TAG = "rtmpVideoMain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
//                R.drawable.bg_main);
//        // 创建Palette对象
//        Palette.generateAsync(bitmap,
//                new Palette.PaletteAsyncListener() {
//                    @Override
//                    public void onGenerated(Palette palette) {
//                        // 通过Palette来获取对应的色调
//                        Palette.Swatch vibrant =
//                                palette.getDarkVibrantSwatch();
//                        // 将颜色设置给相应的组件
//                        getActionBar().setBackgroundDrawable(
//                                new ColorDrawable(vibrant.getRgb()));
//                        Window window = getWindow();
//                        window.setStatusBarColor(vibrant.getRgb());
//                    }
//                });
        playerBtn = (Button) findViewById(R.id.buttonToPlay);
        toTalkBtn = (Button) findViewById(R.id.buttonToTalk);
        settingBtn = (ImageButton) findViewById(R.id.imageButtonToSetting);
        exitBtn = (ImageButton) findViewById(R.id.imageButtonToExit);
        webIndexBtn = (Button) findViewById(R.id.buttonToWebIndex);
        infoShowBtn = (ImageButton) findViewById(R.id.imageButton_main_Sysinfo);
        playerBtn.getBackground().setAlpha(150);
        settingBtn.getBackground().setAlpha(150);
        webIndexBtn.getBackground().setAlpha(150);
        toTalkBtn.getBackground().setAlpha(150);
        exitBtn.getBackground().setAlpha(150);
        playerBtn.setOnClickListener(this);
        settingBtn.setOnClickListener(this);
        toTalkBtn.setOnClickListener(this);
        webIndexBtn.setOnClickListener(this);
        exitBtn.setOnClickListener(this);
        exitBtn.getBackground().setAlpha(150);
        infoShowBtn.setOnClickListener(this);
        //直接进入播放页面
        //MainActivity.this.startActivity(new Intent(MainActivity.this,LivePlayerDemoActivity.class));
        //toTalkBtn.setVisibility(View.GONE);
        checkNetState(this);
    }

    private String getVersionName() throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
        String version = packInfo.versionName;
        return version;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonToPlay:
                MainActivity.this.startActivity(new Intent(MainActivity.this, LivePlayerDemoActivity.class));
                break;
            case R.id.imageButtonToSetting:
                //Log.d("seting", "go to setting activity");
                MainActivity.this.startActivity(new Intent(MainActivity.this, FragmentPreferences.class));
                break;
            case R.id.buttonToTalk:
                //Log.d("talking", "go to talk activity");
                MainActivity.this.startActivity(new Intent(MainActivity.this, LivePublisherDemoActivity.class));
                break;
            case R.id.buttonToWebIndex:
                //Log.d("talking", "go to talk activity");
                MainActivity.this.startActivity(new Intent(MainActivity.this, WebIndexActivity.class));
                break;
            case R.id.imageButtonToExit:
                //Log.d("talking", "go to exit");
                AlertDialog.Builder dialogExit = new AlertDialog.Builder(MainActivity.this);
                dialogExit.setTitle(getString(R.string.exitApp));
                dialogExit.setMessage(getString(R.string.ensureExitApp));
                dialogExit.setCancelable(false);
                dialogExit.setPositiveButton(getString(R.string.continueExitApp), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogExit, int arg1) {
                        // TODO Auto-generated method stub
                        //退出程序
                        System.exit(0);
                    }
                });
                dialogExit.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogExit, int arg1) {
                        // TODO Auto-generated method stub

                    }
                });
                dialogExit.show();
                break;
            case R.id.imageButton_main_Sysinfo:
                //

                final CharSequence[] charSequences = new CharSequence[5];
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                charSequences[0] = getString(R.string.mainAppName);
                charSequences[1] = getString(R.string.mainAppSN)+getString(R.string.app_SN);
                charSequences[2] = getString(R.string.mainAppMaker);
                charSequences[3] = getString(R.string.mainAppContact);
                charSequences[4] = getString(R.string.mainAppEmail);
                builder.setTitle(getString(R.string.mainAppTitle))
                        .setIcon(R.drawable.my_icon)
                        .setItems(charSequences, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this, charSequences[which], Toast.LENGTH_SHORT).show();
                                //	Log.i("abc", "i" + which);
                            }
                        }).show();

                //
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        menu.add(0, menu_setting, 1, "设置").setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(1, menu_toVrActivity, 2, "全景视频播放").setIcon(android.R.drawable.ic_menu_compass);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        super.onOptionsItemSelected(item);
        Intent intent;
        if(item.getItemId()==menu_setting) {
            intent = new Intent(this, FragmentPreferences.class);
            startActivity(intent);
        }
        else if(item.getItemId()==menu_toVrActivity)
        {
           // String url = et.getText().toString();
            String url = "file:///mnt/sdcard/football.mp4";
            if (url.trim()!=""){
                MD360PlayerActivity.startVideo(MainActivity.this, Uri.parse(url));
            } else {
                Toast.makeText(MainActivity.this, "empty url!", Toast.LENGTH_SHORT).show();
            }
            //intent = new Intent(this,WebIndexActivity.class);
        }
        else
        {
            return false;
        }
        return false;
    }
    @Override
    public void onResume()
    {
        super.onResume();
        checkNetState(this);
    }
    public void checkNetState (Context context) {
        ConnectivityManager connectionManager = ( ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isAvailable())
        {
            Toast.makeText(context,getString(R.string.ConnectAvalible),Toast.LENGTH_SHORT).show();
        }
        else {
           // Toast.makeText(context,"network is not avalible",Toast.LENGTH_SHORT).show();
            Toast.makeText(context,getString(R.string.ConnectNotAvalible),Toast.LENGTH_SHORT).show();
        }
    }
}
