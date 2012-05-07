package com.smartvision.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: luhang
 * Date: 11/21/11
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class TaskSite extends Base {
    public static final String TASK_ID = "task_id";
    public static final String SITE_ID = "site_id";
    public static final String IS_FINISHED = "is_finished";

    public static final String[] FIELDS = {TASK_ID, SITE_ID, IS_FINISHED};
    public static final String TABLE = "tasks_sites";
    public static final String CREATE = "create table if not exists " + TABLE + " ("
            + TASK_ID + " INTEGER,"
            + SITE_ID + " INTEGER,"
            + IS_FINISHED + " INTEGER DEFAULT 0)";

    public TaskSite(Context ctx) {
        super(ctx);
        this.table = TABLE;
        this.fields = FIELDS;
    }

    public boolean removeByTaskId(String taskId) {
        open();
        boolean result = db.delete(table, TASK_ID + "=" + taskId, null) > 0;
        close();
        return result;
    }

    public boolean delete(String taskId, String siteId) {
        open();
        boolean result = db.delete(table, TASK_ID + "=" + taskId + " and " + SITE_ID + "=" + siteId, null) > 0;
        close();
        return result;
    }

    public List<Map<String, String>> getByTaskId(String taskId) {
        List<Map<String, String>> sites = new ArrayList<Map<String, String>>();
        StringBuffer sql = new StringBuffer("select ");
        sql.append(Site.ID).append(",").append(Site.NAME).append(",").append(IS_FINISHED);
        sql.append(" from ").append(Site.TABLE).append(",").append(TABLE);
        sql.append(" where ").append(Site.ID).append("=").append(SITE_ID);
        sql.append(" and ").append(TASK_ID).append("=").append(taskId);
        open();
        Cursor cursor = db.rawQuery(sql.toString(), null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Map<String, String> record = new HashMap<String, String>();
            for (String field : Site.FIELDS_FOR_LIST) {
                record.put(field, cursor.getString(cursor.getColumnIndex(field)));
            }
            record.put(IS_FINISHED, cursor.getString(cursor.getColumnIndex(IS_FINISHED)));
            sites.add(record);
            cursor.moveToNext();
        }
        cursor.close();
        close();
        return sites;
    }

    public boolean setFinish(String taskId, String siteId, boolean isFinished) {
        open();
        ContentValues values = new ContentValues();
        values.put(IS_FINISHED, isFinished ? 1 : 0);
        boolean result = db.update(table, values, TASK_ID + "=" + taskId + " and " + SITE_ID + "=" + siteId, null) > 0;
        close();
        return result;
    }

    public boolean isTaskFinished(String taskId) {
        boolean isFinished;
        open();
        Cursor cursor = db.query(true, table, new String[]{TASK_ID}, IS_FINISHED + "=0 and " + TASK_ID + "=" + taskId, null, null, null, null, null);
        isFinished = cursor.getCount() == 0;
        cursor.close();
        close();
        return isFinished;
    }

    public boolean isFinished(String taskId, String siteId) {
        boolean isFinished;
        open();
        Cursor cursor = db.query(true, table, new String[]{IS_FINISHED}, TASK_ID + "=" + taskId + " and " + SITE_ID + "=" + siteId + " and " + IS_FINISHED + "=0", null, null, null, null, null);
        isFinished = cursor.getCount() == 0;
        cursor.close();
        close();
        return isFinished;
    }
}
