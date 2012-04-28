package com.longcent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import com.longcent.utils.Utils;

/**
 * 引导帧界面
 */
public class MyActivity extends Activity {
    private volatile boolean flag;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.startup);
        ImageView imageView = (ImageView) findViewById(R.id.startupImage);
        TextView textView = (TextView) findViewById(R.id.startupVersion);
        textView.setText(getString(R.string.version) + ":" + Utils.getVersionName(this));
        //点击进入主菜单
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                enterMainMenu();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        flag = false;
        autoEnter();
    }

    /**
     * 两秒后不点击直接进入主菜单
     */
    private void autoEnter() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {

                }
                enterMainMenu();
            }
        }).start();
    }

    /**
     * 进入主菜单
     */
    private void enterMainMenu() {
        if (!flag) {
            flag = true;
            startActivity(new Intent("MainMenu"));
        }
    }
}
