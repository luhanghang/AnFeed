package com.longcent;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.*;
import com.longcent.adapters.MediaListToUploadAdapter;
import com.longcent.db.Media;
import com.longcent.db.Site;
import com.longcent.db.Task;
import com.longcent.db.TaskSite;
import com.longcent.utils.FtpTask;

import java.util.List;
import java.util.Map;

/**
 * 待上传任务附件列表界面
 */
public class MediasToUploadActivity extends Activity {
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medias_to_upload);
        Bundle bundle = getIntent().getExtras();
        //先获取所属的任务和站点信息
        final String taskId = bundle.getString(Task.ID);
        final String siteId = bundle.getString(TaskSite.SITE_ID);
        final String key = FtpTask.genKey(taskId, siteId);
        TaskSite taskSite = new TaskSite(this);
        //是否已完成
        final boolean isFinished = taskSite.isFinished(taskId, siteId);

        Media media = new Media(this);
        //获取附件
        final List<Map<String, String>> medias = media.getByTaskId(taskId);

        TextView taskTitle = (TextView) findViewById(R.id.uploadFilesTaskName);
        taskTitle.setText(bundle.getString(Task.TITLE));

        TextView siteName = (TextView) findViewById(R.id.uploadFilesSiteName);
        siteName.setText(bundle.getString(Site.NAME));

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.uploadFilesSiteProgressBar);
        TextView percent = (TextView) findViewById(R.id.uploadFilesSitePercent);

        ImageView playPause = (ImageView) findViewById(R.id.uploadFilesPlayPause);

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        final SiteViewHolder siteViewHolder = new SiteViewHolder();
        siteViewHolder.bar = progressBar;
        siteViewHolder.ftpTask = FtpTask.get(key);
        siteViewHolder.percent = percent;
        siteViewHolder.playPause = playPause;

        //如果已完成设置显示完成
        if (isFinished) {
            if (siteViewHolder.ftpTask == null) {
                siteViewHolder.ftpTask = new FtpTask(this, taskId, siteId);
            }
            siteViewHolder.ftpTask.setFinished();
        }

        ListView listView = (ListView) findViewById(R.id.uploadFiles);
        final MediaListToUploadAdapter uploadFileListAdapter = new MediaListToUploadAdapter(this, medias, taskId, siteId);
        listView.setAdapter(uploadFileListAdapter);

        //点击附件项显示对应的图片或视频
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String uri = medias.get(i).get(Media.URI);
                String intentString = "ImageView";
                if (uri.toString().indexOf("image") < 0) {
                    intentString = "VideoView";
                }
                Intent intent = new Intent(intentString);
                intent.putExtra("uri", Uri.parse(uri));
                startActivity(intent);
            }
        });

        SiteViewHolder.initSiteViewHolder(MediasToUploadActivity.this, siteViewHolder);
        if (siteViewHolder.ftpTask != null) {
            uploadFileListAdapter.setFtpTask(siteViewHolder.ftpTask);
            setHandler(key, siteViewHolder);
        }
        //开始或暂停
        playPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (siteViewHolder.ftpTask == null) {
                    siteViewHolder.ftpTask = new FtpTask(MediasToUploadActivity.this, taskId, siteId);
                    setHandler(key, siteViewHolder);
                }
                switch (siteViewHolder.ftpTask.getState()) {
                    case FtpTask.IDLE:
                    case FtpTask.FINISHED:
                        siteViewHolder.ftpTask.addToQueue();
                        uploadFileListAdapter.setFtpTask(siteViewHolder.ftpTask);
                        break;
                    case FtpTask.PAUSE:
                        siteViewHolder.ftpTask.resume();
                        break;
                    case FtpTask.RUNNING:
                        siteViewHolder.ftpTask.pause();
                        break;
                }
            }
        });
    }

    private void setHandler(final String key, final SiteViewHolder siteViewHolder) {
        Handler handler = new Handler() {
            public void handleMessage(Message message) {
                if (message.what < FtpTask.MESSAGE) {
                    if (message.obj.toString().equals(key)) {
                        switch (message.what) {
                            case FtpTask.STATE:
                                SiteViewHolder.initSiteViewHolder(MediasToUploadActivity.this, siteViewHolder);
                                break;
                            case FtpTask.PROGRESS:
                                SiteViewHolder.setProgress(siteViewHolder, message.arg1);
                                break;
                        }
                    }
                } else {
                    switch (message.what) {
                        case FtpTask.MESSAGE:
                            Toast.makeText(MediasToUploadActivity.this, message.obj.toString(), Toast.LENGTH_LONG).show();
                            break;
                        case FtpTask.WAIT:
                            progressDialog.setMessage(message.obj.toString());
                            try {
                                progressDialog.show();
                            } catch (Exception e) {

                            }
                            break;
                        case FtpTask.STOP_WAIT:
                            progressDialog.dismiss();
                            break;
                    }
                }
            }
        };

        FtpTask.siteHandler = handler;
    }
}
