package com.smartvision.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.longcent.R;
import com.smartvision.TaskActivity;
import com.smartvision.db.Media;
import com.smartvision.db.Site;
import com.smartvision.db.Task;
import com.smartvision.db.TaskSite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 已上传任务列表适配器
 */
public class ListUploadedAdapter extends BaseExpandableListAdapter {
    private LayoutInflater inflater;
    private List<Map<String, String>> groupDataSource;
    private List<List<Map<String, String>>> childrenDataSource;
    private TaskActivity context;
    private TaskSite taskSite;

    public ListUploadedAdapter(TaskActivity context) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        taskSite = new TaskSite(context);
    }

    public void setDataSource(List<Map<String, String>> dataSource) {
        this.groupDataSource = dataSource;
        initChildrenDataSource();
    }

    private void initChildrenDataSource() {
        childrenDataSource = new ArrayList<List<Map<String, String>>>();
        for (Map<String, String> task : groupDataSource) {
            childrenDataSource.add(taskSite.getByTaskId(task.get(Task.ID)));
        }
    }

    public int getGroupCount() {
        return groupDataSource.size();
    }

    public int getChildrenCount(int i) {
        return childrenDataSource.get(i).size();
    }

    public Object getGroup(int i) {
        return groupDataSource.get(i);
    }

    public Object getChild(int i, int i1) {
        return childrenDataSource.get(i).get(i1);
    }

    public long getGroupId(int i) {
        return i;
    }

    public long getChildId(int i, int i1) {
        return i1;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isChildSelectable(int i, int i1) {
        return false;
    }

    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        Map<String, String> taskInf = (Map<String, String>) getGroup(i);
        if (view == null) {
            view = inflater.inflate(R.layout.list_item_uploaded, null);
        }
        TextView title = (TextView) view.findViewById(R.id.list_item_uploaded_task_name);
        title.setText(taskInf.get(Task.TITLE));
        final String taskId = taskInf.get(Task.ID);
        view.setTag(R.id.taskId, taskId);
        ImageView remove = (ImageView) view.findViewById(R.id.list_item_uploaded_remove);
        remove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getString(R.string.confirmDeleteFinishedTask)).setCancelable(false).setPositiveButton(context.getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Media media = new Media(context);
                                media.removeByTaskId(taskId);
                                taskSite.removeByTaskId(taskId);
                                Task task = new Task(context);
                                task.delete(taskId);
                                context.notifyListDataSourceChanged();
                            }
                        }).setNegativeButton(context.getString(R.string.no), null);
                builder.show();
            }
        });
        return view;
    }

    public View getChildView(int groupIndex, int childIndex, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.site_list_item_uploaded, null);
        }
        final TextView siteName = (TextView) view.findViewById(R.id.site_list_item_uploaded_name);
        final Map<String, String> taskInf = (Map<String, String>) getGroup(groupIndex);
        Map<String, String> siteInf = (Map<String, String>) getChild(groupIndex, childIndex);

        final String taskId = taskInf.get(Task.ID);
        final String siteId = siteInf.get(Site.ID);
        siteName.setText(siteInf.get(Site.NAME));

        siteName.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent("UploadedFile");
                intent.putExtra(Task.TITLE, taskInf.get(Task.TITLE));
                intent.putExtra(Site.NAME, siteName.getText());
                intent.putExtra(Task.ID, taskId);
                context.startActivity(intent);
            }
        });

        ImageView reupload = (ImageView) view.findViewById(R.id.site_list_item_upload_reupload);
        reupload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getString(R.string.confirmReupload)).setCancelable(false).setPositiveButton(context.getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Task task = new Task(context);
                                task.setFinish(taskId, false);
                                taskSite.setFinish(taskId, siteId, false);
                                context.notifyListDataSourceChanged();
                            }
                        }).setNegativeButton(context.getString(R.string.no), null);
                builder.show();
            }
        });
        return view;
    }
}