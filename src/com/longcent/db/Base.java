package com.longcent.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作基类
 */
public class Base {
    /**id字段为_id*/
    public static final String ID = "_id";

    protected DatabaseHelper dbHelper;
    protected SQLiteDatabase db;

    protected Context context;
    /**表名*/
    protected String table;
    /**列*/
    protected String[] fields;
    /**列表列*/
    protected String[] fieldsForList;

    protected DatabaseHelper getDbHelper() {
        return new DatabaseHelper(context);
    }

    public Base(Context context) {
        this.context = context;
    }

    public void open() throws SQLException {
        dbHelper = this.getDbHelper();
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * 插入一条数据
     * @param newRecord 新数据包
     * @return 新记录id
     */
    public long create(Bundle newRecord) {
        open();
        ContentValues initialValues = new ContentValues();
        for (String key : newRecord.keySet()) {
            initialValues.put(key, newRecord.getString(key));
        }
        long id = db.insert(table, null, initialValues);
        close();
        return id;
    }

    /**
     * 生成where id in (xxx,xxx)条件字符串
     * @param ids id集合
     * @return 条件字符串
     */
    protected static String genInIdsCondition(List<String> ids) {
        StringBuffer condition = new StringBuffer(ID);
        condition.append(" in (");
        condition.append(ids.get(0));
        for (int i = 1; i < ids.size(); i++) {
            condition.append(",");
            condition.append(ids.get(i));
        }
        condition.append(")");
        return condition.toString();
    }

    /**
     * 删除多条记录
     * @param ids 被删除记录的id集合
     * @return 操作是否成功
     */
    public boolean delete(List<String> ids) {
        open();
        boolean result = db.delete(table, genInIdsCondition(ids), null) > 0;
        close();
        return result;
    }

    /**
     * 删除一条记录
     * @param id
     * @return 操作是否成功
     */
    public boolean delete(String id) {
        open();
        boolean result = db.delete(table, ID + "=" + id, null) > 0;
        close();
        return result;
    }


    /**
     * 取出所有数据
     * @return 数据集合
     */
    public List<Map<String, String>> fetchAll() {
        open();
        List<Map<String, String>> records = new ArrayList<Map<String, String>>();
        String[] fields = fieldsForList == null ? this.fields : this.fieldsForList;
        Cursor cursor = db.query(table, fields, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Map<String, String> record = new HashMap<String, String>();
            for (String field : fields) {
                record.put(field, cursor.getString(cursor.getColumnIndex(field)));
            }
            records.add(record);
            cursor.moveToNext();
        }
        cursor.close();
        close();
        return records;
    }

    /**
     * 根据给定条件取出记录
     * @param condition 条件字符串
     * @return   数据集合
     */
    public List<Map<String, String>> fetchAll(String condition) {
        open();
        List<Map<String, String>> records = new ArrayList<Map<String, String>>();
        String[] fields = fieldsForList == null ? this.fields : this.fieldsForList;
        Cursor cursor = db.query(table, fields, condition, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Map<String, String> record = new HashMap<String, String>();
            for (String field : fields) {
                record.put(field, cursor.getString(cursor.getColumnIndex(field)));
            }
            records.add(record);
            cursor.moveToNext();
        }
        cursor.close();
        close();
        return records;
    }

    /**
     * 取出一条记录
     * @param rowId 记录id
     * @return 记录信息
     * @throws SQLException
     */
    public Bundle fetch(String rowId) throws SQLException {
        open();
        Bundle record = new Bundle();
        Cursor cursor = db.query(true, table, fields, ID + "=" + rowId, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (String field : fields) {
                record.putString(field, cursor.getString(cursor.getColumnIndex(field)));
            }
        }
        if (cursor != null) cursor.close();
        close();
        return record;
    }

    /**
     * 更新一条记录
     * @param rowId 记录id
     * @param record  修改内容
     * @return  更新是否成功
     */
    public boolean update(String rowId, Bundle record) {
        open();
        ContentValues values = new ContentValues();
        for (String key : record.keySet()) {
            values.put(key, record.getString(key));
        }
        boolean result = db.update(table, values, ID + "=" + rowId, null) > 0;
        close();
        return result;
    }
}