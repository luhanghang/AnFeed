package com.smartvision;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * 播放视频
 */
public class VideoViewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_view);
        VideoView videoView = (VideoView) findViewById(R.id.video_view);
        Bundle bundle = getIntent().getExtras();
        videoView.setVideoURI((Uri) bundle.get("uri"));
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.start();
    }
}