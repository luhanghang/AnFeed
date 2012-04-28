package com.longcent;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * 帮助项
 */
public class HelpItemActivity extends Activity {
    String item[];
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item = getResources().getStringArray(R.array.helpItems);
        Bundle bundle = getIntent().getExtras();
        WebView webView = new WebView(this);
        webView.loadData(item[bundle.getInt("index")], "text/html", "utf-8");
        this.setContentView(webView);
    }
}
