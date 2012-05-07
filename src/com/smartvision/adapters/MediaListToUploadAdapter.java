package com.smartvision.adapters;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.smartvision.R;
import com.smartvision.db.Media;
import com.smartvision.utils.FtpTask;
import com.smartvision.utils.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 待上传任务附件列表视频器
 */
public class MediaListToUploadAdapter extends BaseAdapter {
    private Activity context;
    private LayoutInflater inflater;
    private List<Map<String, String>> dataSource;
    private String taskId, siteId;
    private Map<String, FileViewHolder> fileViewHolderMap;
    private FtpTask ftpTask;

    public MediaListToUploadAdapter(Activity context, List<Map<String, String>> dataSource, String taskId, String siteId) {
        this.context = context;
        this.dataSource = dataSource;
        this.taskId = taskId;
        this.siteId = siteId;
        fileViewHolderMap = new HashMap<String, FileViewHolder>();
        this.inflater = LayoutInflater.from(context);
    }

    public void setFtpTask(FtpTask ftpTask) {
        this.ftpTask = ftpTask;
        resetHandler();
    }

    private void resetHandler() {
        if (ftpTask != null) {
            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    FileViewHolder fileViewHolder = fileViewHolderMap.get(msg.obj);
                    if (fileViewHolder != null) {
                        fileViewHolder.setProgress(msg.arg1);
                    }
                }
            };
            FtpTask.mediaHandler = handler;
            handler = null;
        }
    }

    public int getCount() {
        return dataSource.size();
    }

    public Object getItem(int i) {
        return dataSource.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        FileViewHolder viewHolder;
        if (view == null) {
            view = inflater.inflate(R.layout.list_item_medias_to_upload, null);
            viewHolder = new FileViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (FileViewHolder) view.getTag();
        }

        ContentResolver contentResolver = context.getContentResolver();
        Map<String, String> mediaInf = (Map<String, String>) getItem(i);
        Uri uri = Uri.parse(mediaInf.get(Media.URI));
        Bitmap bitmap;
        try {
            if (uri.toString().indexOf("/images/") > 0) {
                viewHolder.play.setAlpha(0);
                bitmap = Utils.getThumbnail(contentResolver, uri, 60);
            } else {
                viewHolder.play.setAlpha(90);
                Uri thumb = Uri.withAppendedPath(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, uri.getLastPathSegment());
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, thumb);
            }
            viewHolder.thumb.setImageBitmap(bitmap);
            File file = Utils.getFileFromUri(context, uri);

            viewHolder.fileName.setText(file.getName());
            viewHolder.fileSize.setText(Utils.convertFileSize(file.length()));
        } catch (Exception e) {
            viewHolder.thumb.setImageResource(R.drawable.invalid_photo);
            viewHolder.fileName.setText(R.string.fileDeleted);
            viewHolder.progressBar.setVisibility(View.GONE);
        }

        String mediaId = mediaInf.get(Media.ID);
        if (ftpTask != null) {
            viewHolder.setProgress(ftpTask.getState() == FtpTask.FINISHED ? 100 : ftpTask.getMediaProgress(mediaId));
        }
        fileViewHolderMap.put(FtpTask.genKey(taskId, siteId, mediaId), viewHolder);
        return viewHolder.view;
    }

    private class FileViewHolder {
        ImageView thumb;
        ProgressBar progressBar;
        TextView percent, fileName, fileSize;
        View view;
        ImageView play;

        public FileViewHolder(View view) {
            this.view = view;
            thumb = (ImageView) view.findViewById(R.id.media_thumb);
            progressBar = (ProgressBar) view.findViewById(R.id.media_progress);
            percent = (TextView) view.findViewById(R.id.media_progress_text);
            fileName = (TextView) view.findViewById(R.id.uploadFileName);
            fileSize = (TextView) view.findViewById(R.id.uploadFileSize);
            play = (ImageView) view.findViewById(R.id.media_play);
        }

        private void setProgress(int progress) {
            progressBar.setProgress(progress);
            switch (progress) {
                case 0:
                    percent.setText(context.getString(R.string.waiting));
                    break;
                case 100:
                    percent.setText(context.getString(R.string.done));
                    break;
                default:
                    percent.setText(progress + "%");
            }
        }
    }
}
