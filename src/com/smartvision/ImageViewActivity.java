package com.smartvision;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

/**
 *显示单张照片
 */
public class ImageViewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view);
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        Bundle bundle = getIntent().getExtras();
        imageView.setImageURI((Uri) bundle.get("uri"));
    }
}
