package com.smartvision;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.*;
import com.smartvision.adapters.ListToUploadAdapter;
import com.smartvision.adapters.ListUploadedAdapter;
import com.smartvision.db.Task;
import com.smartvision.utils.FtpTask;
import com.smartvision.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * 任务管理界面
 */
public class TaskActivity extends Activity {
    public final static String ALL = "all";
    public final static String TOUPLOAD = "to_upload";
    public final static String UPLOADED = "uploaded";

    private TabHost tabHost;
    private Task task;
    private List<Map<String, String>> toUploads, uploadeds;
    private ListToUploadAdapter toUploadsAdapter;
    private ListUploadedAdapter uploadedsAdapter;
    private TabHost.TabSpec specUploadeds, specToUploads;
    private TextView totalUploadeds, totalToUploads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);
        //标签栏
        tabHost = (TabHost) this.findViewById(R.id.taskTabHost);
        tabHost.setup();

        task = new Task(this);

        TabHost.TabSpec spec = tabHost.newTabSpec(ALL);
        spec.setContent(R.id.allTasks);
        spec.setIndicator(this.getString(R.string.all));
        tabHost.addTab(spec);

        specToUploads = tabHost.newTabSpec(TOUPLOAD);
        specToUploads.setContent(R.id.toUploadList);
        specToUploads.setIndicator("");
        tabHost.addTab(specToUploads);

        specUploadeds = tabHost.newTabSpec(UPLOADED);
        specUploadeds.setContent(R.id.uploadedList);
        specUploadeds.setIndicator("");
        tabHost.addTab(specUploadeds);

        totalUploadeds = (TextView) findViewById(R.id.totalUploaded);
        totalToUploads = (TextView) findViewById(R.id.totalToUpload);

        toUploadsAdapter = new ListToUploadAdapter(this);
        uploadedsAdapter = new ListUploadedAdapter(this);

        setListDataSource();

        setExpandableListView(R.id.toUploadInAll, toUploadsAdapter);
        setExpandableListView(R.id.uploadedInAll, uploadedsAdapter);
        setExpandableListView(R.id.toUploadList, toUploadsAdapter);
        setExpandableListView(R.id.uploadedList, uploadedsAdapter);

        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                notifyListDataSourceChanged();
            }
        };
        FtpTask.mainHandler = handler;
    }

    public void notifyListDataSourceChanged() {
        setListDataSource();
        toUploadsAdapter.notifyDataSetInvalidated();
        uploadedsAdapter.notifyDataSetInvalidated();
    }

    private void setListDataSource() {
        toUploads = task.getToUploads();
        uploadeds = task.getUploadeds();

        toUploadsAdapter.setDataSource(toUploads);
        uploadedsAdapter.setDataSource(uploadeds);

        Utils.changeTabTitle(tabHost, 1, this.getString(R.string.toUpload) + " (" + toUploads.size() + ")");
        Utils.changeTabTitle(tabHost, 2, this.getString(R.string.uploaded) + " (" + uploadeds.size() + ")");

        totalUploadeds.setText(String.format(getString(R.string.totalItems), uploadeds.size()));
        totalToUploads.setText(String.format(getString(R.string.totalItems), toUploads.size()));
    }

    public void setExpandableListView(int resourceId, ExpandableListAdapter adapter) {
        final boolean readOnly = resourceId == R.id.uploadedInAll || resourceId == R.id.uploadedList;
        ExpandableListView view = (ExpandableListView) findViewById(resourceId);
        view.setAdapter(adapter);
        //长按任务项编辑任务信息
        view.setOnItemLongClickListener(new ExpandableListView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return showUploadActivity(view, readOnly);
            }
        });
        //短按展开
        view.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                if (expandableListView.isGroupExpanded(i)) {
                    expandableListView.collapseGroup(i);
                } else {
                    expandableListView.expandGroup(i);
                }
                return true;
            }
        });
    }

    private boolean showUploadActivity(View view, boolean readOnly) {
        String taskId = (String) view.getTag(R.id.taskId);
        if (taskId == null) return false;
        Intent intent = new Intent("Upload");
        intent.putExtra("readOnly", readOnly);
        intent.putExtra(Task.ID, taskId);
        startActivityForResult(intent, 0);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            toUploadsAdapter.setDataSource(task.getToUploads());
            toUploadsAdapter.notifyDataSetInvalidated();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        toUploadsAdapter.resetHandler();
    }
}
