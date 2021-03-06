package com.example.gamebrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wei.mark.standout.Utils;


public class FullScreenActivity extends Activity {
    public HashMap<String,String> clipboardFinder = new HashMap<>();
    public static String baseUrl = SettingActivity.defaultPage;

    public static boolean isRunning = false;

    WebView mWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen);
        isRunning = true;
        rootView = findViewById(R.id.rootView);
        FrameLayout frame = rootView;
        baseUrl = Utils.getSP(this).getString("url",SettingActivity.defaultPage);

        if(null==mWebView) {
            this.mWebView = new WebView(this);
            WebGameBoostEngine.boost(this,mWebView,baseUrl,null);
        }
        frame.addView(mWebView);
        renderW = Utils.getSP(this).getInt("rw",854);
        renderH = Utils.getSP(this).getInt("rh",480);
        //this.mWebView.setWebViewClient(new baseUrl(this));

        hWnd.postDelayed(resizer,1000);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mWebView.getLayoutParams();
        frame.setClickable(true);
        lp.width=renderW;
        lp.height = renderH;
        mWebView.setLayoutParams(lp);
        lp.gravity = Gravity.CENTER;
        rootView = frame;



        mWebView.loadUrl(baseUrl);

    }

    int renderW=854,renderH=480;


    Runnable resizer = new Runnable() {
        @Override
        public void run() {
            hWnd.removeCallbacks(this);
            if(null!=rootView){
                if(rootView.getWidth()>0) {
                    scaleView();
                    return;
                }
            }
            hWnd.postDelayed(this,1000);
        }
    };


    FrameLayout rootView;
    void scaleView(){
        float pw = rootView.getWidth();
        float ph = rootView.getHeight();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mWebView.getLayoutParams();
        if(pw==0 || ph==0){
            Log.e("SCALE","View not initialized");
            hWnd.postDelayed(resizer,1000);
            return;
        }
        {
            lp.width=renderW;
            lp.height = renderH;
            if(pw / renderW * renderH < ph){
                //屏幕更高的场合
                mWebView.setScaleX(pw/renderW);
                mWebView.setScaleY(pw/renderW);
            }
            else{
                //屏幕更窄的场合
                mWebView.setScaleX(ph/renderH);
                mWebView.setScaleY(ph/renderH);
            }

        }



        mWebView.setLayoutParams(lp);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(null!=rootView){
            hWnd.postDelayed(resizer,1000);
        }
    }


    Handler hWnd = new Handler();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        mWebView.destroy();
    }

    public void onHide() {
        //4.1及以上通用flags组合
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    flags | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus){
            onHide();
            hWnd.postDelayed(resizer,1000);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("是否退出？").setPositiveButton("是的", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).setNegativeButton("不是",null).setNeutralButton("刷新页面", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mWebView.loadUrl(baseUrl);
            }
        }).create().show();
    }
}
