package com.longcent.adapters;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.longcent.R;
import com.longcent.db.Media;
import com.longcent.utils.Utils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 已上传任务附件列表适配器
 */
public class MediaListUploadedAdapter extends BaseAdapter {
    private Activity context;
    private LayoutInflater inflater;
    private List<Map<String, String>> dataSource;

    public MediaListUploadedAdapter(Activity context, List<Map<String, String>> dataSource) {
        this.context = context;
        this.dataSource = dataSource;
        this.inflater = LayoutInflater.from(context);
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
        if (view == null) {
            view = inflater.inflate(R.layout.list_item_medias_uploaded, null);
        }

        ContentResolver contentResolver = context.getContentResolver();
        Map<String, String> mediaInf = (Map<String, String>) getItem(i);
        Uri uri = Uri.parse(mediaInf.get(Media.URI));
        ImageView thumbView = (ImageView) view.findViewById(R.id.media_thumb_uploaded);
        ImageView play = (ImageView) view.findViewById(R.id.media_uploaded_play);
        TextView fileName = (TextView) view.findViewById(R.id.uploadedFileName);
        TextView fileSize = (TextView) view.findViewById(R.id.uploadedFileSize);
        try {
            Bitmap bitmap;
            if (uri.toString().indexOf("/images/") > 0) {
                play.setAlpha(0);
                bitmap = Utils.getThumbnail(contentResolver, uri, 60);
            } else {
                play.setAlpha(90);
                Uri thumb = Uri.withAppendedPath(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, uri.getLastPathSegment());
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, thumb);
            }
            thumbView.setImageBitmap(bitmap);
            File file = Utils.getFileFromUri(context, uri);
            fileName.setText(file.getName());
            fileSize.setText(Utils.convertFileSize(file.length()) + " ");
        } catch (Exception e) {
            thumbView.setImageResource(R.drawable.invalid_photo);
            fileName.setText(R.string.fileDeleted);
        }
        return view;
    }
}
