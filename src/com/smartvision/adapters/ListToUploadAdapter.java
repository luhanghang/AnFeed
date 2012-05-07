package com.smartvision.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.longcent.R;
import com.smartvision.SiteViewHolder;
import com.smartvision.TaskActivity;
import com.smartvision.db.Media;
import com.smartvision.db.Site;
import com.smartvision.db.Task;
import com.smartvision.db.TaskSite;
import com.smartvision.utils.FtpTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 待上传列表适配器
 */
public class ListToUploadAdapter extends BaseExpandableListAdapter {
    private LayoutInflater inflater;
    /**任务列表数据源*/
    private List<Map<String, String>> groupDataSource;
    /**任务下站点数据源*/
    private List<List<Map<String, String>>> childrenDataSource;
    private TaskSite taskSite;
    private TaskActivity context;
    private Map<String, SiteViewHolder> siteViewHolderMap = new HashMap<String, SiteViewHolder>();
    private ProgressDialog progressDialog;

    public ListToUploadAdapter(TaskActivity context) {
        this.inflater = LayoutInflater.from(context);
        taskSite = new TaskSite(context);
        this.context = context;
        initProgressDialog();
        resetHandler();
    }

    /**
     * 初始化进度窗口,用作提示登录状态
     */
    private void initProgressDialog() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    /**
     * 重设Handler
     */
    public void resetHandler() {
        for (String key : siteViewHolderMap.keySet()) {
            SiteViewHolder siteViewHolder = siteViewHolderMap.get(key);
            siteViewHolder.ftpTask = FtpTask.get(key);
            SiteViewHolder.initSiteViewHolder(context, siteViewHolder);
        }
        Handler handler = new Handler() {
            public void handleMessage(Message message) {
                if (message.what < FtpTask.MESSAGE) {  //提示状态和进度
                    SiteViewHolder siteViewHolder = siteViewHolderMap.get(message.obj);
                    if (siteViewHolder == null) return;
                    switch (message.what) {
                        case FtpTask.STATE:
                            SiteViewHolder.initSiteViewHolder(context, siteViewHolder);
                            break;
                        case FtpTask.PROGRESS:
                            SiteViewHolder.setProgress(siteViewHolder, message.arg1);
                            break;
                    }
                } else { //提示消息和等待
                    switch (message.what) {
                        case FtpTask.MESSAGE:
                            Toast.makeText(context, message.obj.toString(), Toast.LENGTH_LONG).show();
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

    /**
     * 设置数据源
     * @param dataSource 任务列表数据源
     */
    public void setDataSource(List<Map<String, String>> dataSource) {
        this.groupDataSource = dataSource;
        initChildrenDataSource();
    }

    /**
     * 设置任务下站点数据源
     */
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

    /**
     * 生成GroupView
     * @param i
     * @param b
     * @param view
     * @param viewGroup
     * @return
     */
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        TextView title;
        Map<String, String> task = (Map<String, String>) getGroup(i);
        if (view == null) {
            view = inflater.inflate(R.layout.list_item_to_upload, null);
        }
        title = (TextView) view.findViewById(R.id.list_item_to_upload_task_name);
        title.setText(task.get(Task.TITLE));

        final String taskId = task.get(Task.ID);

        ImageView remove = (ImageView) view.findViewById(R.id.list_item_toupload_remove);
        remove.setOnClickListener(new View.OnClickListener() { //删除任务
            public void onClick(View view) {
                FtpTask ftpTask = FtpTask.getSitesRunning(); //如果任务处在上传或等待过程中不执行删除操作
                if (ftpTask == null || ftpTask.getTaskId() != taskId || (ftpTask.getState() != FtpTask.RUNNING && ftpTask.getState() != FtpTask.QUEUE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(context.getString(R.string.confirmDeleteFinishedTask)).setCancelable(false).setPositiveButton(context.getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Media media = new Media(context);
                                    media.removeByTaskId(taskId); //先删除任务下的附件
                                    taskSite.removeByTaskId(taskId); //再删除任务下站点
                                    Task task = new Task(context);
                                    task.delete(taskId);          //最后删除任务
                                    context.notifyListDataSourceChanged(); //通知数据源改变
                                }
                            }).setNegativeButton(context.getString(R.string.no), null);
                    builder.show();
                }
            }
        });
        view.setTag(R.id.taskId, taskId);
        return view;
    }

    /**
     * 设置ChildView
     * @param groupIndex
     * @param childIndex
     * @param b
     * @param view
     * @param viewGroup
     * @return
     */
    public View getChildView(int groupIndex, int childIndex, boolean b, View view, ViewGroup viewGroup) {
        Map<String, String> siteInf = (Map<String, String>) getChild(groupIndex, childIndex);
        Map<String, String> taskInf = (Map<String, String>) getGroup(groupIndex);
        String taskId = taskInf.get(Task.ID);
        String siteId = siteInf.get(Site.ID);
        String key = FtpTask.genKey(taskId, siteId);

        SiteViewHolder siteViewHolder = siteViewHolderMap.get(key);
        if (siteViewHolder == null) {
            view = inflater.inflate(R.layout.site_list_item_to_upload, null);
            siteViewHolder = new SiteViewHolder();
            siteViewHolder.view = view;
            siteViewHolder.siteName = (TextView) view.findViewById(R.id.site_list_item_name);
            siteViewHolder.percent = (TextView) view.findViewById(R.id.site_progress_text);
            siteViewHolder.bar = (ProgressBar) view.findViewById(R.id.site_progress);
            siteViewHolder.removeButton = (ImageView) view.findViewById(R.id.button_site_delete);
            siteViewHolder.playPause = (ImageView) view.findViewById(R.id.button_play_pause);
        }
        setData(siteViewHolder, groupIndex, childIndex);
        return siteViewHolder.view;
    }

    /**
     * 设置站点列表项数据
     * @param siteViewHolder
     * @param groupIndex
     * @param childIndex
     */
    public void setData(final SiteViewHolder siteViewHolder, int groupIndex, int childIndex) {
        Map<String, String> siteInf = (Map<String, String>) getChild(groupIndex, childIndex);
        final Map<String, String> taskInf = (Map<String, String>) getGroup(groupIndex);
        siteViewHolder.siteName.setText(siteInf.get(Site.NAME));
        final String taskId = taskInf.get(Task.ID);
        final String siteId = siteInf.get(Site.ID);
        final int idx = groupIndex, idx1 = childIndex;

        final String key = FtpTask.genKey(taskId, siteId);
        siteViewHolderMap.put(key, siteViewHolder);
        siteViewHolder.ftpTask = FtpTask.get(key);
        final boolean isFinished = siteInf.get(TaskSite.IS_FINISHED).equals("1");
        if (isFinished) {
            if (siteViewHolder.ftpTask == null) {
                siteViewHolder.ftpTask = new FtpTask(context, taskId, siteId);
            }
            siteViewHolder.ftpTask.setFinished();
        }

        SiteViewHolder.initSiteViewHolder(context, siteViewHolder);

        /**
         * 点击站点名称进入附件列表
         */
        siteViewHolder.siteName.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent("UploadFile");
                intent.putExtra(Task.TITLE, taskInf.get(Task.TITLE));
                intent.putExtra(Site.NAME, siteViewHolder.siteName.getText());
                intent.putExtra(Task.ID, taskId);
                intent.putExtra(TaskSite.SITE_ID, siteId);
                context.startActivity(intent);
            }
        });

        /**
         * 开始暂停上传
         */
        siteViewHolder.playPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (siteViewHolder.ftpTask == null) {
                    siteViewHolder.ftpTask = new FtpTask(context, taskId, siteId);
                }
                switch (siteViewHolder.ftpTask.getState()) {
                    case FtpTask.IDLE:
                    case FtpTask.FINISHED:
                        siteViewHolder.ftpTask.addToQueue();
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

        /**
         * 删除站点
         */
        siteViewHolder.removeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getString(R.string.confirmDelete)).setCancelable(false).setPositiveButton(context.getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String siteId = childrenDataSource.get(idx).get(idx1).get(Site.ID);
                                taskSite.delete(taskId, siteId);
                                childrenDataSource.get(idx).remove(idx1);
                                if (siteViewHolder.ftpTask != null) {
                                    siteViewHolder.ftpTask.delete();
                                }
                                siteViewHolderMap.remove(key);
                                notifyDataSetInvalidated();
                            }
                        }).setNegativeButton(context.getString(R.string.no), null);
                builder.show();
            }
        });
    }
}