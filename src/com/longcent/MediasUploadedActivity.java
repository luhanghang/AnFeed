package com.longcent;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.longcent.adapters.MediaListUploadedAdapter;
import com.longcent.db.Media;
import com.longcent.db.Site;
import com.longcent.db.Task;

import java.util.List;
import java.util.Map;

/**
 * 已上传任务附件列表界面
 */
public class MediasUploadedActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medias_uploaded);
        Bundle bundle = getIntent().getExtras();

        String taskId = bundle.getString(Task.ID);
        Media media = new Media(this);
        final List<Map<String, String>> medias = media.getByTaskId(taskId);

        TextView taskTitle = (TextView) findViewById(R.id.uploadedFilesTaskName);
        taskTitle.setText(bundle.getString(Task.TITLE));

        TextView siteName = (TextView) findViewById(R.id.uploadedFilesSiteName);
        siteName.setText(bundle.getString(Site.NAME));

        ListView listView = (ListView) findViewById(R.id.uploadedFiles);
        listView.setAdapter(new MediaListUploadedAdapter(this, medias));
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
    }
}
