package com.longcent.db;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.longcent.utils.Utils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 附件
 */
public class Media extends Base {
    /**附件uri*/
    public static final String URI = "uri";
    /**附件所属任务id*/
    public static final String TASK_ID = "task_id";
    /**远程文件名*/
    public static final String REMOTE_NAME = "remote_name";

    public static final String[] FIELDS = {ID, URI, TASK_ID, REMOTE_NAME};
    public static final String[] FIELDS_FOR_LIST = {ID, URI, REMOTE_NAME};
    public static final String TABLE = "medias";
    /**建表脚本*/
    public static final String CREATE = "create table if not exists " + TABLE + " ("
            + ID + " INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,"
            + URI + " VARCHAR NOT NULL , "
            + REMOTE_NAME + " VARCHAR,"
            + TASK_ID + " INTEGER)";

    public Media(Context ctx) {
        super(ctx);
        this.table = TABLE;
        this.fields = FIELDS;
        this.fieldsForList = FIELDS_FOR_LIST;
    }

    /**
     * 删除id为taskId的附件
     * @param taskId
     * @return 删除是否成功
     */
    public boolean removeByTaskId(String taskId) {
        open();
        boolean result = db.delete(table, TASK_ID + "=" + taskId, null) > 0;
        close();
        return result;
    }

    /**
     * 获取id为taskId的任务的附件
     * @param taskId
     * @return 附件集合
     */
    public List<Map<String, String>> getByTaskId(String taskId) {
        return fetchAll(TASK_ID + "=" + taskId);
    }

    /**
     * 生成远程文件名 规则为:电话_任务id_原文件名
     * @param context
     * @param taskId 任务id
     * @param uri  附件uri
     * @return  远程文件名
     */
    public static String genRemoteName(Activity context, String taskId, Uri uri) {
        File file = Utils.getFileFromUri(context, uri);
        StringBuffer remoteName = new StringBuffer();
        remoteName.append(Utils.getDeviceInf(context).getLine1Number()).append("_").append(taskId).append("_").append(file.getName());
        return remoteName.toString();
    }
}
