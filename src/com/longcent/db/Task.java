package com.longcent.db;

import android.content.ContentValues;
import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 * 任务信息
 */
public class Task extends Base {
    /**
     * 标题
     */
    public static final String TITLE = "title";
    /**
     * 关键字
     */
    public static final String KEYWORDS = "keywords";
    /**
     * 地点
     */
    public static final String LOCATION = "location";
    /**
     * 时间
     */
    public static final String CREATED_TIME = "created_time";
    /**
     * 分类
     */
    public static final String CATEGORY = "category";
    /**
     * 上传时间
     */
    public static final String UPLOADED_TIME = "uploaded_time";
    /**
     * 是否完成
     */
    public static final String IS_FINISHED = "is_finished";

    public static final String[] FIELDS = {ID, TITLE, KEYWORDS, LOCATION, CREATED_TIME, CATEGORY, UPLOADED_TIME, IS_FINISHED};
    public static final String[] FIELDS_FOR_LIST = {ID, TITLE, UPLOADED_TIME, IS_FINISHED};
    public static final String TABLE = "tasks";
    public static final String CREATE = "create table if not exists " + TABLE + " ("
            + ID + " INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,"
            + TITLE + " VARCHAR NOT NULL , "
            + KEYWORDS + " VARCHAR, "
            + LOCATION + " VARCHAR, "
            + CATEGORY + " VARCHAR, "
            + CREATED_TIME + " VARCHAR, "
            + UPLOADED_TIME + " VARCHAR,"
            + IS_FINISHED + " INTEGER DEFAULT 0)";

    public Task(Context ctx) {
        super(ctx);
        this.table = TABLE;
        this.fields = FIELDS;
        this.fieldsForList = FIELDS_FOR_LIST;
    }

    /**
     * 获得未上传任务
     *
     * @return 未上传任务列表
     */
    public List<Map<String, String>> getToUploads() {
        return fetchAll(IS_FINISHED + "=0");
    }

    /**
     * 获得已上传任务
     *
     * @return 已上传任务列表
     */
    public List<Map<String, String>> getUploadeds() {
        return fetchAll(IS_FINISHED + "=1");
    }

    /*
    删除任务多个
     */
    @Override
    public boolean delete(List<String> ids) {
        Media media = new Media(context);
        TaskSite taskSite = new TaskSite(context);
        for (String id : ids) {
            media.removeByTaskId(id);
            taskSite.removeByTaskId(id);
        }
        return super.delete(ids);
    }

    /**
     * 设置任务是否完成
     * @param taskId
     * @param isFinished
     * @return
     */
    public boolean setFinish(String taskId, boolean isFinished) {
        open();
        ContentValues values = new ContentValues();
        values.put(IS_FINISHED, isFinished ? 1 : 0);
        boolean result = db.update(table, values, ID + "=" + taskId, null) > 0;
        close();
        return result;
    }
}